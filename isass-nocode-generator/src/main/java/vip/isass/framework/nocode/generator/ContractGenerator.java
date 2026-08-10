// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.generator;

import com.thoughtworks.qdox.JavaProjectBuilder;
import com.thoughtworks.qdox.model.DocletTag;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaMethod;
import com.thoughtworks.qdox.model.JavaParameter;
import com.thoughtworks.qdox.model.JavaType;
import tools.jackson.databind.ObjectMapper;
import vip.isass.framework.nocode.contract.ContractDocument;
import vip.isass.framework.nocode.contract.HttpMethod;
import vip.isass.framework.nocode.contract.OperationContract;
import vip.isass.framework.nocode.contract.ParameterContract;
import vip.isass.framework.nocode.contract.ParameterSource;
import vip.isass.framework.nocode.contract.ServiceContract;
import vip.isass.framework.nocode.contract.StandardContractFactory;
import vip.isass.framework.nocode.contract.PropertyContract;
import vip.isass.framework.nocode.contract.TypeContract;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ContractGenerator {

    private static final Pattern SERVICE_TYPE = Pattern.compile(
            ".*IService<([^,]+),\\s*([^>]+)>.*");
    private static final Pattern PATH_VARIABLE = Pattern.compile("\\{([^}]+)}");

    private final ObjectMapper objectMapper;

    public ContractGenerator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ContractDocument generate(Path sourceRoot, Path outputRoot) throws Exception {
        JavaProjectBuilder builder = new JavaProjectBuilder();
        builder.addSourceTree(sourceRoot.toFile());
        List<ServiceContract> services = builder.getClasses().stream()
                .filter(JavaClass::isInterface)
                .filter(type -> type.getSimpleName().startsWith("I")
                        && type.getSimpleName().endsWith("Service")
                        && !type.getSimpleName().equals("IService"))
                .filter(type -> extendsService(type) || extendsApplicationService(type))
                .map(this::toService)
                .toList();
        LinkedHashSet<String> documentedTypes = new LinkedHashSet<>();
        services.forEach(service -> {
            addDocumentedTypes(documentedTypes, service.entityJavaType());
            service.operations().forEach(operation -> {
                operation.parameters().forEach(parameter ->
                        addDocumentedTypes(documentedTypes, parameter.javaType()));
                addDocumentedTypes(documentedTypes, operation.returnJavaType());
            });
        });
        boolean expanded;
        do {
            int sizeBefore = documentedTypes.size();
            List<String> currentTypes = List.copyOf(documentedTypes);
            currentTypes.stream()
                    .map(builder::getClassByName)
                    .filter(java.util.Objects::nonNull)
                    .flatMap(type -> type.getFields().stream())
                    .forEach(field -> addDocumentedTypes(
                            documentedTypes, field.getType().getGenericFullyQualifiedName()));
            expanded = documentedTypes.size() > sizeBefore;
        } while (expanded);
        List<TypeContract> types = documentedTypes.stream()
                .map(builder::getClassByName)
                .filter(java.util.Objects::nonNull)
                .map(this::toType)
                .toList();

        ContractDocument unhashed = new ContractDocument(
                ContractDocument.CURRENT_VERSION, "", services, types);
        String hash = sha256(objectMapper.writeValueAsBytes(unhashed));
        ContractDocument document = new ContractDocument(
                ContractDocument.CURRENT_VERSION, hash, services, types);
        writeContract(outputRoot, document);
        writeProto(outputRoot, document);
        return document;
    }

    private String rawJavaType(String javaType) {
        int generic = javaType.indexOf('<');
        return generic < 0 ? javaType.replace("[]", "") : javaType.substring(0, generic);
    }

    /** 收集实体、请求与响应的实际类型；泛型容器本身会在后续查找中自然过滤。 */
    private void addDocumentedTypes(LinkedHashSet<String> types, String javaType) {
        for (String candidate : javaType.replace("[]", "").split("[<>,?\\s]+")) {
            if (!candidate.isBlank()) {
                types.add(candidate);
            }
        }
    }

    private TypeContract toType(JavaClass type) {
        List<PropertyContract> properties = type.getFields().stream()
                .filter(field -> !field.isStatic() && !field.isTransient())
                .map(field -> new PropertyContract(
                        field.getName(),
                        field.getType().getGenericFullyQualifiedName(),
                        field.getComment()))
                .toList();
        return new TypeContract(
                type.getFullyQualifiedName(),
                type.getSimpleName(),
                type.getComment(),
                properties);
    }

    private boolean extendsService(JavaClass type) {
        return type.getImplements().stream()
                .map(JavaType::getGenericFullyQualifiedName)
                .anyMatch(value -> value.contains("IService<"));
    }

    private boolean extendsApplicationService(JavaClass type) {
        return type.getImplements().stream()
                .map(JavaType::getFullyQualifiedName)
                .anyMatch(value -> value.equals("vip.isass.framework.nocode.service.IApplicationService"));
    }

    private ServiceContract toService(JavaClass type) {
        if (extendsApplicationService(type)) {
            return toApplicationService(type);
        }
        JavaType parent = type.getImplements().stream()
                .filter(value -> value.getGenericFullyQualifiedName().contains("IService<"))
                .findFirst()
                .orElseThrow();
        Matcher matcher = SERVICE_TYPE.matcher(parent.getGenericFullyQualifiedName());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Cannot resolve IService types: " + type.getFullyQualifiedName());
        }
        String entityType = matcher.group(1).trim();
        String criteriaType = matcher.group(2).trim();
        String entitySimpleName = entityType.substring(entityType.lastIndexOf('.') + 1);
        String entity = entitySimpleName;
        entity = Character.toLowerCase(entity.charAt(0)) + entity.substring(1);
        String[] packageParts = type.getPackageName().split("\\.");
        String applicationName = packageParts.length >= 3 ? packageParts[2] + "-service" : "application";
        List<OperationContract> operations = new ArrayList<>(
                StandardContractFactory.operations(entityType, criteriaType));
        operations.addAll(type.getMethods().stream()
                .map(method -> toOperation(type, method))
                .toList());
        return new ServiceContract(
                applicationName,
                entity,
                type.getFullyQualifiedName(),
                entityType,
                criteriaType,
                stringTag(type, "tag", type.getSimpleName()),
                operations);
    }

    private ServiceContract toApplicationService(JavaClass type) {
        String simpleName = type.getSimpleName().replaceFirst("^I", "").replaceFirst("Service$", "");
        String entity = Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
        String[] packageParts = type.getPackageName().split("\\.");
        String applicationName = packageParts.length >= 3 ? packageParts[2] + "-service" : "application";
        List<OperationContract> operations = type.getMethods().stream()
                .map(method -> toOperation(type, method))
                .toList();
        return new ServiceContract(
                applicationName,
                entity,
                type.getFullyQualifiedName(),
                Object.class.getName(),
                Object.class.getName(),
                stringTag(type, "tag", type.getSimpleName()),
                operations);
    }

    private String stringTag(JavaClass type, String name, String defaultValue) {
        DocletTag tag = type.getTagByName(name);
        return tag == null || tag.getValue().isBlank() ? defaultValue : tag.getValue().trim();
    }

    private OperationContract toOperation(JavaClass serviceType, JavaMethod method) {
        HttpTag http = parseHttp(serviceType, method);
        List<ParameterContract> parameters = new ArrayList<>();
        for (JavaParameter parameter : method.getParameters()) {
            ParameterSource source = parameterSource(http, parameter.getName());
            parameters.add(new ParameterContract(
                    parameter.getName(),
                    parameter.getResolvedGenericFullyQualifiedName(),
                    source,
                    source == ParameterSource.PATH,
                    parameterDescription(method, parameter.getName())));
        }
        int order = integerTag(method, "order", 1000);
        return new OperationContract(
                method.getName(),
                http.method(),
                http.path(),
                order,
                http.method() == HttpMethod.GET,
                parameters,
                method.getReturnType().getGenericFullyQualifiedName(),
                method.getComment());
    }

    private HttpTag parseHttp(JavaClass serviceType, JavaMethod method) {
        DocletTag tag = method.getTagByName("http");
        if (tag == null || tag.getValue().isBlank()) {
            throw new IllegalArgumentException("Custom nocode method requires @http METHOD /path: "
                    + serviceType.getFullyQualifiedName() + "#" + method.getName());
        }
        String[] parts = tag.getValue().trim().split("\\s+", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("@http requires METHOD and PATH: " + method.getName());
        }
        return new HttpTag(HttpMethod.valueOf(parts[0].toUpperCase(Locale.ROOT)), parts[1]);
    }

    private ParameterSource parameterSource(HttpTag http, String name) {
        Matcher matcher = PATH_VARIABLE.matcher(http.path());
        while (matcher.find()) {
            if (matcher.group(1).equals(name)) {
                return ParameterSource.PATH;
            }
        }
        return http.method() == HttpMethod.GET || http.method() == HttpMethod.DELETE
                ? ParameterSource.QUERY
                : ParameterSource.BODY;
    }

    private String parameterDescription(JavaMethod method, String name) {
        return method.getTagsByName("param").stream()
                .map(DocletTag::getValue)
                .filter(value -> value.equals(name) || value.startsWith(name + " "))
                .map(value -> value.length() == name.length() ? "" : value.substring(name.length()).trim())
                .findFirst()
                .orElse("");
    }

    private int integerTag(JavaMethod method, String name, int defaultValue) {
        DocletTag tag = method.getTagByName(name);
        return tag == null ? defaultValue : Integer.parseInt(tag.getValue().trim());
    }

    private void writeContract(Path outputRoot, ContractDocument document) throws IOException {
        Path target = outputRoot.resolve("META-INF/isass/nocode-contract.json");
        Files.createDirectories(target.getParent());
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(target.toFile(), document);
    }

    private void writeProto(Path outputRoot, ContractDocument document) throws IOException {
        Map<String, List<ServiceContract>> applications = new LinkedHashMap<>();
        document.services().forEach(service -> applications
                .computeIfAbsent(service.service(), ignored -> new ArrayList<>()).add(service));
        for (var applicationServices : applications.entrySet()) {
            String application = applicationServices.getKey().replaceFirst("-service$", "");
            Path target = outputRoot.resolve("proto/" + application + "-nocode.proto");
            Files.createDirectories(target.getParent());
            Files.writeString(target, proto(applicationServices.getValue()), StandardCharsets.UTF_8);
        }
    }

    private String proto(List<ServiceContract> services) {
        String servicePackage = services.getFirst().serviceInterface();
        String packageName = servicePackage
                .substring(0, servicePackage.lastIndexOf('.'))
                .replaceFirst("\\.api(?:\\..*)?$", ".nocode");
        StringBuilder proto = new StringBuilder("""
                syntax = "proto3";
                package %s;

                """.formatted(packageName));
        proto.append("message Request { bytes payload = 1; }\n");
        proto.append("message Response { bytes payload = 1; }\n");
        for (ServiceContract contract : services) {
            String grpcService = contract.serviceInterface()
                    .substring(contract.serviceInterface().lastIndexOf('.') + 1)
                    .replaceFirst("^I", "");
            proto.append("\nservice ").append(grpcService).append(" {\n");
            for (OperationContract operation : contract.operations()) {
                String methodName = upperFirst(operation.name());
                proto.append("  rpc ").append(methodName)
                        .append("(Request) returns (Response);\n");
            }
            proto.append("}\n");
        }
        return proto.toString();
    }

    private String upperFirst(String value) {
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private record HttpTag(HttpMethod method, String path) {
    }
}
