package vip.isass.framework.nocode.v3.contract;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class V3ContractRegistry {

    private final Map<String, V3ServiceContract> services;

    public V3ContractRegistry(List<V3ServiceContract> contracts) {
        Map<String, V3ServiceContract> indexed = new LinkedHashMap<>();
        for (V3ServiceContract contract : contracts) {
            String key = key(contract.serviceName(), contract.entityName());
            if (indexed.putIfAbsent(key, contract) != null) {
                throw new IllegalArgumentException("Duplicate V3 service contract: " + key);
            }
        }
        this.services = Map.copyOf(indexed);
    }

    public V3ServiceContract requireService(String serviceName, String entityName) {
        V3ServiceContract contract = services.get(key(serviceName, entityName));
        if (contract == null) {
            throw new IllegalArgumentException("Unknown V3 service: " + serviceName + "/" + entityName);
        }
        return contract;
    }

    public List<V3ServiceContract> contracts() {
        return List.copyOf(services.values());
    }

    public V3OperationContract requireOperation(
            String serviceName,
            String entityName,
            V3HttpMethod method,
            String relativePath
    ) {
        String normalized = relativePath.startsWith("/") ? relativePath : "/" + relativePath;
        return requireService(serviceName, entityName).operations().stream()
                .filter(operation -> operation.httpMethod() == method)
                .filter(operation -> V3RouteMatcher.matches(operation.path(), normalized))
                .sorted(Comparator
                        .comparingInt(V3ContractRegistry::staticSegmentCount).reversed()
                        .thenComparingInt(V3ContractRegistry::variableSegmentCount)
                        .thenComparingInt(V3OperationContract::order))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown V3 operation: " + method + " " + normalized));
    }

    private static int staticSegmentCount(V3OperationContract operation) {
        int count = 0;
        for (String segment : segments(operation.path())) {
            if (!isVariableSegment(segment)) {
                count++;
            }
        }
        return count;
    }

    private static int variableSegmentCount(V3OperationContract operation) {
        int count = 0;
        for (String segment : segments(operation.path())) {
            if (isVariableSegment(segment)) {
                count++;
            }
        }
        return count;
    }

    private static boolean isVariableSegment(String segment) {
        return segment.startsWith("{") && segment.endsWith("}");
    }

    private static List<String> segments(String path) {
        String trimmed = path.replaceAll("^/+|/+$", "");
        return trimmed.isEmpty() ? List.of() : List.of(trimmed.split("/"));
    }

    private String key(String serviceName, String entityName) {
        return serviceName + '\n' + entityName;
    }
}
