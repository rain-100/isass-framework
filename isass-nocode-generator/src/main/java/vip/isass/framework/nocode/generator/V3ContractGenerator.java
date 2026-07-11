package vip.isass.framework.nocode.generator;

import com.thoughtworks.qdox.JavaProjectBuilder;
import com.thoughtworks.qdox.model.DocletTag;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaMethod;
import com.thoughtworks.qdox.model.JavaParameter;
import com.thoughtworks.qdox.model.JavaType;
import tools.jackson.databind.ObjectMapper;
import vip.isass.framework.nocode.v3.contract.V3ContractDocument;
import vip.isass.framework.nocode.v3.contract.V3HttpMethod;
import vip.isass.framework.nocode.v3.contract.V3OperationContract;
import vip.isass.framework.nocode.v3.contract.V3ParameterContract;
import vip.isass.framework.nocode.v3.contract.V3ParameterSource;
import vip.isass.framework.nocode.v3.contract.V3ServiceContract;
import vip.isass.framework.nocode.v3.contract.V3StandardContractFactory;
import vip.isass.framework.nocode.v3.contract.V3PropertyContract;
import vip.isass.framework.nocode.v3.contract.V3TypeContract;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class V3ContractGenerator {

    private static final Pattern SERVICE_TYPE = Pattern.compile(
            ".*IV3Service<([^,]+),\\s*([^>]+)>.*");
    private static final Pattern PATH_VARIABLE = Pattern.compile("\\{([^}]+)}");

    private final ObjectMapper objectMapper;

    public V3ContractGenerator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public V3ContractDocument generate(Path sourceRoot, Path outputRoot) throws Exception {
        JavaProjectBuilder builder = new JavaProjectBuilder();
        builder.addSourceTree(sourceRoot.toFile());
        List<V3ServiceContract> services = builder.getClasses().stream()
                .filter(JavaClass::isInterface)
                .filter(type -> type.getSimpleName().startsWith("IV3")
                        && type.getSimpleName().endsWith("Service"))
                .filter(this::extendsV3Service)
                .map(this::toService)
                .toList();
        List<V3TypeContract> types = services.stream()
                .map(V3ServiceContract::entityJavaType)
                .distinct()
                .map(builder::getClassByName)
                .filter(java.util.Objects::nonNull)
                .map(this::toType)
                .toList();

        V3ContractDocument unhashed = new V3ContractDocument(
                V3ContractDocument.CURRENT_VERSION, "", services, types);
        String hash = sha256(objectMapper.writeValueAsBytes(unhashed));
        V3ContractDocument document = new V3ContractDocument(
                V3ContractDocument.CURRENT_VERSION, hash, services, types);
        writeContract(outputRoot, document);
        writeProto(outputRoot, document);
        return document;
    }

    private V3TypeContract toType(JavaClass type) {
        List<V3PropertyContract> properties = type.getFields().stream()
                .filter(field -> !field.isStatic() && !field.isTransient())
                .map(field -> new V3PropertyContract(
                        field.getName(),
                        field.getType().getGenericFullyQualifiedName(),
                        field.getComment()))
                .toList();
        return new V3TypeContract(
                type.getFullyQualifiedName(),
                type.getSimpleName(),
                type.getComment(),
                properties);
    }

    private boolean extendsV3Service(JavaClass type) {
        return type.getImplements().stream()
                .map(JavaType::getGenericFullyQualifiedName)
                .anyMatch(value -> value.contains("IV3Service<"));
    }

    private V3ServiceContract toService(JavaClass type) {
        JavaType parent = type.getImplements().stream()
                .filter(value -> value.getGenericFullyQualifiedName().contains("IV3Service<"))
                .findFirst()
                .orElseThrow();
        Matcher matcher = SERVICE_TYPE.matcher(parent.getGenericFullyQualifiedName());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Cannot resolve IV3Service types: " + type.getFullyQualifiedName());
        }
        String entityType = matcher.group(1).trim();
        String criteriaType = matcher.group(2).trim();
        String entitySimpleName = entityType.substring(entityType.lastIndexOf('.') + 1);
        String entityName = entitySimpleName.replaceFirst("^V3", "");
        entityName = Character.toLowerCase(entityName.charAt(0)) + entityName.substring(1);
        String[] packageParts = type.getPackageName().split("\\.");
        String applicationName = packageParts.length >= 3 ? packageParts[2] + "-service" : "application";
        List<V3OperationContract> operations = new ArrayList<>(
                V3StandardContractFactory.operations(entityType, criteriaType));
        operations.addAll(type.getMethods().stream()
                .map(method -> toOperation(type, method))
                .toList());
        return new V3ServiceContract(
                applicationName,
                entityName,
                type.getFullyQualifiedName(),
                entityType,
                criteriaType,
                stringTag(type, "tag", type.getSimpleName()),
                operations);
    }

    private String stringTag(JavaClass type, String name, String defaultValue) {
        DocletTag tag = type.getTagByName(name);
        return tag == null || tag.getValue().isBlank() ? defaultValue : tag.getValue().trim();
    }

    private V3OperationContract toOperation(JavaClass serviceType, JavaMethod method) {
        HttpTag http = parseHttp(serviceType, method);
        List<V3ParameterContract> parameters = new ArrayList<>();
        for (JavaParameter parameter : method.getParameters()) {
            V3ParameterSource source = parameterSource(http, parameter.getName());
            parameters.add(new V3ParameterContract(
                    parameter.getName(),
                    parameter.getResolvedGenericFullyQualifiedName(),
                    source,
                    source == V3ParameterSource.PATH,
                    parameterDescription(method, parameter.getName())));
        }
        int order = integerTag(method, "order", 1000);
        return new V3OperationContract(
                method.getName(),
                http.method(),
                http.path(),
                order,
                http.method() == V3HttpMethod.GET,
                parameters,
                method.getReturnType().getGenericFullyQualifiedName(),
                method.getComment());
    }

    private HttpTag parseHttp(JavaClass serviceType, JavaMethod method) {
        DocletTag tag = method.getTagByName("http");
        if (tag == null || tag.getValue().isBlank()) {
            throw new IllegalArgumentException("Custom V3 method requires @http METHOD /path: "
                    + serviceType.getFullyQualifiedName() + "#" + method.getName());
        }
        String[] parts = tag.getValue().trim().split("\\s+", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("@http requires METHOD and PATH: " + method.getName());
        }
        return new HttpTag(V3HttpMethod.valueOf(parts[0].toUpperCase(Locale.ROOT)), parts[1]);
    }

    private V3ParameterSource parameterSource(HttpTag http, String name) {
        Matcher matcher = PATH_VARIABLE.matcher(http.path());
        while (matcher.find()) {
            if (matcher.group(1).equals(name)) {
                return V3ParameterSource.PATH;
            }
        }
        return http.method() == V3HttpMethod.GET || http.method() == V3HttpMethod.DELETE
                ? V3ParameterSource.QUERY
                : V3ParameterSource.BODY;
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

    private void writeContract(Path outputRoot, V3ContractDocument document) throws IOException {
        Path target = outputRoot.resolve("META-INF/isass/v3-contract.json");
        Files.createDirectories(target.getParent());
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(target.toFile(), document);
    }

    private void writeProto(Path outputRoot, V3ContractDocument document) throws IOException {
        Map<String, List<V3ServiceContract>> applications = new LinkedHashMap<>();
        document.services().forEach(service -> applications
                .computeIfAbsent(service.serviceName(), ignored -> new ArrayList<>()).add(service));
        for (var applicationServices : applications.entrySet()) {
            String application = applicationServices.getKey().replaceFirst("-service$", "");
            Path target = outputRoot.resolve("proto/" + application + "-v3.proto");
            Files.createDirectories(target.getParent());
            Files.writeString(target, proto(applicationServices.getValue()), StandardCharsets.UTF_8);
        }
    }

    private String proto(List<V3ServiceContract> services) {
        String servicePackage = services.getFirst().serviceInterface();
        String packageName = servicePackage
                .substring(0, servicePackage.lastIndexOf('.'))
                .replaceFirst("\\.api(?:\\..*)?$", ".v3");
        StringBuilder proto = new StringBuilder("""
                syntax = "proto3";
                package %s;

                """.formatted(packageName));
        proto.append("message V3Request { bytes payload = 1; }\n");
        proto.append("message V3Response { bytes payload = 1; }\n");
        for (V3ServiceContract service : services) {
            String serviceName = service.serviceInterface()
                    .substring(service.serviceInterface().lastIndexOf('.') + 1)
                    .replaceFirst("^IV3", "");
            proto.append("\nservice ").append(serviceName).append(" {\n");
            for (V3OperationContract operation : service.operations()) {
                String methodName = upperFirst(operation.name());
                proto.append("  rpc ").append(methodName)
                        .append("(V3Request) returns (V3Response);\n");
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

    private record HttpTag(V3HttpMethod method, String path) {
    }
}
