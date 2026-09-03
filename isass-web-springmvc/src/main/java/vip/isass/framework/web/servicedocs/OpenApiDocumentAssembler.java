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
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Merges service-isolated Smart-doc resources with local Entrypoint metadata. */
public final class OpenApiDocumentAssembler {

    private static final String RESOURCE_PATTERN =
            "classpath*:META-INF/isass/openapi/*/openapi.json";
    private static final String NOCODE_TAG = "零代码接口";

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
        List<ServiceDefinition> services = registry.all().stream()
                .filter(ServiceDefinition::localImplementation)
                .filter(this::inScope)
                .sorted(Comparator.comparingInt(ServiceDefinition::displayOrder)
                        .thenComparing(ServiceDefinition::key))
                .toList();
        appendEntrypointTags(target, services);
        services.forEach(service -> appendCustomEntrypoints(target, service));
        appendNocodeEntrypoints(target, services);
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

    private void appendCustomEntrypoints(ObjectNode root, ServiceDefinition service) {
        for (OperationDefinition operation : service.operations()) {
            if (operation.nocode()) continue;
            appendConcreteEntrypoint(root, service, operation);
        }
    }

    private void appendEntrypointTags(ObjectNode root, List<ServiceDefinition> services) {
        Map<String, Integer> tagOrders = new LinkedHashMap<>();
        for (ServiceDefinition service : services) {
            if (service.operations().stream().anyMatch(operation -> !operation.nocode())) {
                tagOrders.merge(service.tag(), service.displayOrder(), Math::min);
            }
        }
        if (services.stream().anyMatch(service ->
                service.operations().stream().anyMatch(OperationDefinition::nocode))) {
            tagOrders.put(NOCODE_TAG, 1);
        }
        if (tagOrders.isEmpty()) return;

        ArrayNode tags = root.putArray("tags");
        tagOrders.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue()
                        .thenComparing(entry -> NOCODE_TAG.equals(entry.getKey()) ? 0 : 1)
                        .thenComparing(Map.Entry.comparingByKey()))
                .forEach(entry -> tags.addObject()
                        .put("name", entry.getKey())
                        .put("x-order", entry.getValue()));
    }

    private void appendConcreteEntrypoint(ObjectNode root, ServiceDefinition service,
                                          OperationDefinition operation) {
        ObjectNode paths = root.withObject("/paths");
        ObjectNode schemas = root.withObject("/components").withObject("/schemas");
        String path = service.pathPrefix(operation) + "/" + operation.operationName();
        ObjectNode api = operationNode(paths, path, operation);
        api.put("operationId", service.serviceName() + "_" + service.contextName() + "_"
                + service.resourceName() + "_" + operation.operationName());
        api.putArray("tags").add(service.tag());
        ArrayNode parameters = api.putArray("parameters");
        for (ParameterDefinition parameter : operation.parameters()) {
            appendParameter(api, parameters, schemas, parameter);
        }
        appendResponse(api, schemaFor(operation.returnType(), schemas, new HashSet<>()));
    }

    private void appendNocodeEntrypoints(ObjectNode root, List<ServiceDefinition> services) {
        Map<String, List<NocodeBinding>> grouped = new LinkedHashMap<>();
        for (ServiceDefinition service : services) {
            for (OperationDefinition operation : service.operations()) {
                if (!operation.nocode()) continue;
                grouped.computeIfAbsent(operation.operationName(), ignored -> new ArrayList<>())
                        .add(new NocodeBinding(service, operation));
            }
        }
        grouped.values().stream()
                .sorted(Comparator
                        .comparingInt((List<NocodeBinding> bindings) -> bindings.getFirst().operation().displayOrder())
                        .thenComparing(bindings -> bindings.getFirst().operation().operationName()))
                .forEach(bindings -> appendNocodeEntrypoint(root, bindings));
    }

    private void appendNocodeEntrypoint(ObjectNode root, List<NocodeBinding> bindings) {
        OperationDefinition canonical = bindings.getFirst().operation();
        validateNocodeBindings(bindings, canonical);

        ObjectNode paths = root.withObject("/paths");
        ObjectNode schemas = root.withObject("/components").withObject("/schemas");
        String path = "/{service}/nocode/{entity}/" + canonical.operationName();
        ObjectNode api = operationNode(paths, path, canonical);
        api.put("operationId", "nocode_" + canonical.operationName());
        api.putArray("tags").add(NOCODE_TAG);

        ArrayNode parameters = api.putArray("parameters");
        parameters.add(pathParameter("service"));
        parameters.add(pathParameter("entity"));

        ObjectNode serviceEntities = api.putObject("x-isass-service-entities");
        ObjectNode entityOptions = api.putObject("x-isass-entity-options");
        ObjectNode criteriaParameters = api.putObject("x-isass-criteria-parameters");
        Map<String, ObjectNode> uniqueParameters = new LinkedHashMap<>();
        Map<String, ObjectNode> requestSchemas = new LinkedHashMap<>();
        Map<String, ObjectNode> responseSchemas = new LinkedHashMap<>();
        Map<String, String> entityLabels = entityLabels(bindings);
        Map<String, String> requestSchemaEntities = new LinkedHashMap<>();

        for (NocodeBinding binding : bindings) {
            String entity = binding.entity();
            addUnique(serviceEntities.withArray(binding.service().serviceName()), entity);
            entityOptions.put(entityLabels.get(binding.key()), entity);
            ObjectNode entityCriteria = literalObjectProperty(
                    criteriaParameters, entity, "NoCode Criteria 参数映射");

            ObjectNode temporaryApi = objectMapper.createObjectNode();
            ArrayNode temporaryParameters = objectMapper.createArrayNode();
            for (ParameterDefinition parameter : binding.operation().parameters()) {
                if (parameter.source() == ParameterSource.BODY) {
                    ObjectNode schema = schemaFor(parameter.javaType(), schemas, new HashSet<>());
                    requestSchemas.putIfAbsent(schema.toString(), schema);
                    String schemaName = referencedSchemaName(schema);
                    if (schemaName != null) requestSchemaEntities.put(schemaName, entity);
                    continue;
                }
                appendParameter(temporaryApi, temporaryParameters, schemas, parameter);
            }
            for (JsonNode parameter : temporaryParameters) {
                String key = parameter.path("in").asText() + ":" + parameter.path("name").asText();
                uniqueParameters.putIfAbsent(key, (ObjectNode) parameter.deepCopy());
                entityCriteria.set(parameter.path("name").asText(), parameter.deepCopy());
            }
            ObjectNode responseSchema = schemaFor(binding.operation().returnType(), schemas, new HashSet<>());
            responseSchemas.putIfAbsent(responseSchema.toString(), responseSchema);
        }
        uniqueParameters.values().forEach(parameters::add);
        appendAggregatedRequestBody(api, requestSchemas.values(), requestSchemaEntities);
        appendResponse(api, combinedSchema(responseSchemas.values()));
    }

    private ObjectNode operationNode(ObjectNode paths, String path, OperationDefinition operation) {
        ObjectNode pathItem = literalObjectProperty(paths, path, "OpenAPI path");
        String method = operation.httpMethod().name().toLowerCase(Locale.ROOT);
        if (pathItem.has(method)) {
            throw new IllegalStateException("Entrypoint OpenAPI 路由冲突: " + method + " " + path);
        }
        ObjectNode api = pathItem.putObject(method);
        api.put("summary", operation.displayName());
        api.put("description", operation.description());
        api.put("x-order", operation.displayOrder());
        return api;
    }

    private void appendResponse(ObjectNode api, ObjectNode schema) {
        ObjectNode response = api.putObject("responses").putObject("200");
        response.put("description", "成功");
        response.putObject("content").putObject("application/json").set("schema", schema);
    }

    private ObjectNode pathParameter(String name) {
        ObjectNode parameter = objectMapper.createObjectNode();
        parameter.put("name", name);
        parameter.put("in", "path");
        parameter.put("required", true);
        parameter.putObject("schema").put("type", "string");
        return parameter;
    }

    private void appendAggregatedRequestBody(ObjectNode api, Collection<ObjectNode> candidates,
                                             Map<String, String> schemaEntities) {
        if (candidates.isEmpty()) return;
        ObjectNode media = api.putObject("requestBody").put("required", true)
                .putObject("content").putObject("application/json");
        media.set("schema", combinedSchema(candidates));
        ObjectNode mapping = media.putObject("x-isass-oneof-mapping");
        schemaEntities.forEach(mapping::put);
    }

    private ObjectNode combinedSchema(Collection<ObjectNode> candidates) {
        if (candidates.isEmpty()) return objectMapper.createObjectNode();
        if (candidates.size() == 1) return candidates.iterator().next().deepCopy();
        ObjectNode schema = objectMapper.createObjectNode();
        ArrayNode oneOf = schema.putArray("oneOf");
        candidates.forEach(candidate -> oneOf.add(candidate.deepCopy()));
        return schema;
    }

    private String referencedSchemaName(ObjectNode schema) {
        JsonNode reference = schema.get("$ref");
        if (reference == null && schema.path("type").asText().equals("array")) {
            reference = schema.path("items").get("$ref");
        }
        if (reference == null || !reference.isTextual()) return null;
        String value = reference.asText();
        return value.substring(value.lastIndexOf('/') + 1);
    }

    private Map<String, String> entityLabels(List<NocodeBinding> bindings) {
        Map<String, Long> counts = bindings.stream().collect(Collectors.groupingBy(
                binding -> binding.service().resourceName(), LinkedHashMap::new, Collectors.counting()));
        Map<String, String> labels = new LinkedHashMap<>();
        for (NocodeBinding binding : bindings) {
            String resourceName = binding.service().resourceName();
            String label = resourceName;
            // Entity is a frontend-facing value. Keep it in the generated Java
            // small-camel form instead of exposing a localized business label.
            // Only add a camel-case scope when two resources share the same name.
            if (counts.getOrDefault(resourceName, 0L) > 1) {
                label = toLowerCamel(binding.service().contextName())
                        + upperFirst(resourceName);
                if (labels.containsValue(label)) {
                    label = toLowerCamel(binding.service().serviceName())
                            + upperFirst(toLowerCamel(binding.service().contextName()))
                            + upperFirst(resourceName);
                }
            }
            labels.put(binding.key(), label);
        }
        return labels;
    }

    private String upperFirst(String value) {
        if (value == null || value.isBlank()) return value;
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private String toLowerCamel(String value) {
        if (value == null || value.isBlank()) return value;
        StringBuilder result = new StringBuilder();
        boolean capitalize = false;
        for (char character : value.toCharArray()) {
            if (!Character.isLetterOrDigit(character)) {
                capitalize = result.length() > 0;
                continue;
            }
            if (result.isEmpty()) {
                result.append(Character.toLowerCase(character));
            } else if (capitalize) {
                result.append(Character.toUpperCase(character));
                capitalize = false;
            } else {
                result.append(character);
            }
        }
        return result.toString();
    }

    private void addUnique(ArrayNode values, String value) {
        for (JsonNode existing : values) {
            if (existing.asText().equals(value)) return;
        }
        values.add(value);
    }

    private void validateNocodeBindings(List<NocodeBinding> bindings, OperationDefinition canonical) {
        for (NocodeBinding binding : bindings) {
            OperationDefinition operation = binding.operation();
            if (operation.httpMethod() != canonical.httpMethod()) {
                throw new IllegalStateException("NoCode 标准操作 HTTP 方法不一致: " + canonical.operationName());
            }
        }
    }

    private ObjectNode literalObjectProperty(ObjectNode parent, String propertyName, String kind) {
        JsonNode existing = parent.get(propertyName);
        if (existing == null) return parent.putObject(propertyName);
        if (existing instanceof ObjectNode object) return object;
        throw new IllegalStateException(kind + " 必须是对象: " + propertyName);
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
            node.put("explode", false);
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

    private record NocodeBinding(ServiceDefinition service, OperationDefinition operation) {
        String entity() {
            return service.contextName() + "/" + service.resourceName();
        }

        String key() {
            return service.key();
        }
    }
}
