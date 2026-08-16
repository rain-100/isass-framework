// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.servicedocs;

import org.springframework.core.ResolvableType;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import vip.isass.framework.entrypoint.metadata.OperationDefinition;
import vip.isass.framework.entrypoint.metadata.ParameterDefinition;
import vip.isass.framework.entrypoint.metadata.ParameterSource;
import vip.isass.framework.entrypoint.metadata.ServiceDefinition;
import vip.isass.framework.entrypoint.registry.ServiceDefinitionRegistry;

import java.beans.Introspector;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Merges service-isolated Smart-doc resources with local Entrypoint metadata. */
public final class OpenApiDocumentAssembler {

    private static final String RESOURCE_PATTERN =
            "classpath*:META-INF/isass/openapi/*/openapi.json";

    private final ObjectMapper objectMapper;
    private final ServiceDefinitionRegistry registry;
    private final Environment environment;

    public OpenApiDocumentAssembler(ObjectMapper objectMapper,
                                    ServiceDefinitionRegistry registry,
                                    Environment environment) {
        this.objectMapper = objectMapper;
        this.registry = registry;
        this.environment = environment;
    }

    public String assemble() {
        ObjectNode target = emptyDocument();
        loadStaticDocuments().forEach(source -> mergeDocument(target, source));
        registry.all().stream()
                .filter(ServiceDefinition::localImplementation)
                .filter(this::inScope)
                .sorted(Comparator.comparing(ServiceDefinition::key))
                .forEach(service -> appendEntrypoint(target, service));
        try {
            return objectMapper.writeValueAsString(target);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("序列化 OpenAPI 失败", exception);
        }
    }

    private ObjectNode emptyDocument() {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("openapi", "3.0.3");
        ObjectNode info = root.putObject("info");
        info.put("title", environment.getProperty("spring.application.name", "isass"));
        info.put("version", environment.getProperty("info.version", "4.0.0"));
        root.putObject("paths");
        root.putObject("components").putObject("schemas");
        return root;
    }

    private List<ObjectNode> loadStaticDocuments() {
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver().getResources(RESOURCE_PATTERN);
            List<ObjectNode> documents = new ArrayList<>();
            for (Resource resource : resources) {
                String serviceName = serviceName(resource);
                if (microserviceMode() && !applicationName().equals(serviceName)) continue;
                JsonNode parsed = objectMapper.readTree(resource.getInputStream());
                if (!(parsed instanceof ObjectNode object)) {
                    throw new IllegalStateException("OpenAPI 根节点必须是对象: " + resource);
                }
                documents.add(object);
            }
            return documents;
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private String serviceName(Resource resource) throws IOException {
        String url = resource.getURL().toString();
        String marker = "/META-INF/isass/openapi/";
        int start = url.lastIndexOf(marker);
        if (start < 0) throw new IllegalStateException("OpenAPI 资源路径不合法: " + url);
        String suffix = url.substring(start + marker.length());
        return suffix.substring(0, suffix.indexOf('/'));
    }

    private void mergeDocument(ObjectNode target, ObjectNode source) {
        mergeUniqueObject(target.withObject("/paths"), source.path("paths"), "OpenAPI path");
        JsonNode schemas = source.path("components").path("schemas");
        mergeUniqueObject(target.withObject("/components").withObject("/schemas"), schemas, "OpenAPI schema");
    }

    private void mergeUniqueObject(ObjectNode target, JsonNode source, String kind) {
        if (!source.isObject()) return;
        source.properties().forEach(entry -> {
            JsonNode existing = target.get(entry.getKey());
            if (existing != null && !existing.equals(entry.getValue())) {
                throw new IllegalStateException(kind + " 冲突: " + entry.getKey());
            }
            if (existing == null) target.set(entry.getKey(), entry.getValue().deepCopy());
        });
    }

    private void appendEntrypoint(ObjectNode root, ServiceDefinition service) {
        ObjectNode paths = root.withObject("/paths");
        ObjectNode schemas = root.withObject("/components").withObject("/schemas");
        for (OperationDefinition operation : service.operations()) {
            String path = service.pathPrefix(operation) + "/" + operation.operationName();
            ObjectNode pathItem = paths.withObject(path);
            String method = operation.httpMethod().name().toLowerCase(Locale.ROOT);
            if (pathItem.has(method)) {
                throw new IllegalStateException("Entrypoint OpenAPI 路由冲突: " + method + " " + path);
            }
            ObjectNode api = pathItem.putObject(method);
            api.put("summary", operation.displayName());
            api.put("description", operation.description());
            api.put("operationId", service.serviceName() + "_" + service.contextName() + "_"
                    + service.resourceName() + "_" + operation.operationName());
            api.put("x-order", operation.displayOrder());
            ArrayNode tags = api.putArray("tags");
            tags.add(service.contextName() + "/" + service.resourceName());
            ArrayNode parameters = api.putArray("parameters");
            for (ParameterDefinition parameter : operation.parameters()) {
                appendParameter(api, parameters, schemas, parameter);
            }
            ObjectNode response = api.putObject("responses").putObject("200");
            response.put("description", "成功");
            response.putObject("content").putObject("application/json")
                    .set("schema", schemaFor(operation.returnType(), schemas, new HashSet<>()));
        }
    }

    private void appendParameter(ObjectNode api, ArrayNode parameters, ObjectNode schemas,
                                 ParameterDefinition parameter) {
        if (parameter.source() == ParameterSource.BODY) {
            api.putObject("requestBody").put("required", true)
                    .putObject("content").putObject("application/json")
                    .set("schema", schemaFor(parameter.javaType(), schemas, new HashSet<>()));
            return;
        }
        if (parameter.source() == ParameterSource.QUERY && parameter.objectQuery()) {
            Class<?> raw = rawClass(parameter.javaType());
            try {
                for (var property : Introspector.getBeanInfo(raw, Object.class).getPropertyDescriptors()) {
                    if (property.getReadMethod() == null || property.getWriteMethod() == null) continue;
                    Type propertyType = property.getReadMethod().getGenericReturnType();
                    parameters.add(parameterNode(property.getName(), "query", propertyType, schemas));
                }
            } catch (java.beans.IntrospectionException exception) {
                throw new IllegalStateException("解析 Query 对象失败: " + raw.getName(), exception);
            }
            return;
        }
        String location = switch (parameter.source()) {
            case QUERY -> "query";
            case HEADER -> "header";
            case FORM_FIELD, FORM_FILE -> "query";
            case BODY -> throw new IllegalStateException();
        };
        parameters.add(parameterNode(parameter.name(), location, parameter.javaType(), schemas));
    }

    private ObjectNode parameterNode(String name, String in, Type type, ObjectNode schemas) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("name", name);
        node.put("in", in);
        node.put("required", false);
        node.set("schema", schemaFor(type, schemas, new HashSet<>()));
        if (isCollection(type)) {
            node.put("style", "form");
            node.put("explode", true);
        }
        return node;
    }

    private ObjectNode schemaFor(Type type, ObjectNode schemas, Set<Type> visiting) {
        Set<String> schemaNames = new HashSet<>();
        visiting.forEach(value -> schemaNames.add(schemaName(ResolvableType.forType(value))));
        return schemaFor(ResolvableType.forType(type), schemas, schemaNames);
    }

    private ObjectNode schemaFor(ResolvableType type, ObjectNode schemas, Set<String> visiting) {
        Class<?> raw = type.resolve(Object.class);
        ObjectNode schema = objectMapper.createObjectNode();
        if (raw == void.class || raw == Void.class) return schema;
        if (raw == Object.class) {
            schema.put("type", "object");
            return schema;
        }
        if (raw.isArray() || CollectionLike.isCollection(raw)) {
            ResolvableType itemType = raw.isArray() ? type.getComponentType() : type.getGeneric(0);
            schema.put("type", "array");
            schema.set("items", schemaFor(itemType, schemas, visiting));
            return schema;
        }
        if (Map.class.isAssignableFrom(raw)) {
            schema.put("type", "object");
            schema.set("additionalProperties", schemaFor(type.getGeneric(1), schemas, visiting));
            return schema;
        }
        String primitive = primitiveType(raw);
        if (primitive != null) {
            schema.put("type", primitive);
            if (raw.isEnum()) {
                ArrayNode values = schema.putArray("enum");
                for (Object constant : raw.getEnumConstants()) values.add(String.valueOf(constant));
            }
            return schema;
        }
        String name = schemaName(type);
        schema.put("$ref", "#/components/schemas/" + name);
        if (schemas.has(name) || !visiting.add(name)) return schema;
        ObjectNode definition = schemas.putObject(name);
        definition.put("type", "object");
        ObjectNode properties = definition.putObject("properties");
        try {
            var beanInfo = raw.isInterface()
                    ? Introspector.getBeanInfo(raw)
                    : Introspector.getBeanInfo(raw, Object.class);
            for (var property : beanInfo.getPropertyDescriptors()) {
                if (property.getReadMethod() == null) continue;
                properties.set(property.getName(), schemaFor(
                        ResolvableType.forType(property.getReadMethod().getGenericReturnType(), type),
                        schemas, visiting));
            }
        } catch (java.beans.IntrospectionException exception) {
            throw new IllegalStateException("解析 OpenAPI Schema 失败: " + raw.getName(), exception);
        }
        visiting.remove(name);
        return schema;
    }

    private String schemaName(ResolvableType type) {
        Class<?> raw = type.resolve(Object.class);
        StringBuilder name = new StringBuilder(normalizeSchemaName(raw.getName()));
        for (ResolvableType generic : type.getGenerics()) {
            name.append("__").append(schemaName(generic));
        }
        return name.toString();
    }

    private String normalizeSchemaName(String name) {
        return name.replace('.', '_').replace('$', '_');
    }

    private boolean inScope(ServiceDefinition service) {
        return !microserviceMode() || applicationName().equals(service.serviceName());
    }

    private boolean microserviceMode() {
        return environment.getProperty("isass.boot.microservice.enabled", Boolean.class, false);
    }

    private String applicationName() {
        String name = environment.getProperty("spring.application.name");
        if (microserviceMode() && (name == null || name.isBlank())) {
            throw new IllegalStateException("微服务模式必须配置 spring.application.name");
        }
        return name == null ? "" : name;
    }

    private boolean isCollection(Type type) {
        Class<?> raw = rawClass(type);
        return raw.isArray() || CollectionLike.isCollection(raw);
    }

    private Class<?> rawClass(Type type) {
        if (type instanceof Class<?> raw) return raw;
        if (type instanceof ParameterizedType p && p.getRawType() instanceof Class<?> raw) return raw;
        return Object.class;
    }

    private String primitiveType(Class<?> type) {
        if (type == String.class || type == Character.class || type == char.class
                || type.isEnum() || java.time.temporal.Temporal.class.isAssignableFrom(type)
                || type == java.util.UUID.class) return "string";
        if (type == boolean.class || type == Boolean.class) return "boolean";
        if (type == byte.class || type == short.class || type == int.class || type == long.class
                || Number.class.isAssignableFrom(type)) return "integer";
        if (type == float.class || type == double.class) return "number";
        return null;
    }

    private static final class CollectionLike {
        static boolean isCollection(Class<?> type) {
            return Iterable.class.isAssignableFrom(type) || java.util.Collection.class.isAssignableFrom(type);
        }
    }
}
