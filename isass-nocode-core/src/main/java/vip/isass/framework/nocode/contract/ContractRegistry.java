package vip.isass.framework.nocode.contract;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ContractRegistry {

    private final Map<String, ServiceContract> services;

    public ContractRegistry(List<ServiceContract> contracts) {
        Map<String, ServiceContract> indexed = new LinkedHashMap<>();
        for (ServiceContract contract : contracts) {
            String key = key(contract.serviceName(), contract.entityName());
            if (indexed.putIfAbsent(key, contract) != null) {
                throw new IllegalArgumentException("Duplicate  service contract: " + key);
            }
        }
        this.services = Map.copyOf(indexed);
    }

    public ServiceContract requireService(String serviceName, String entityName) {
        ServiceContract contract = services.get(key(serviceName, entityName));
        if (contract == null) {
            throw new IllegalArgumentException("Unknown  service: " + serviceName + "/" + entityName);
        }
        return contract;
    }

    public List<ServiceContract> contracts() {
        return List.copyOf(services.values());
    }

    public OperationContract requireOperation(
            String serviceName,
            String entityName,
            HttpMethod method,
            String relativePath
    ) {
        String normalized = relativePath.startsWith("/") ? relativePath : "/" + relativePath;
        return requireService(serviceName, entityName).operations().stream()
                .filter(operation -> operation.httpMethod() == method)
                .filter(operation -> RouteMatcher.matches(operation.path(), normalized))
                .sorted(Comparator
                        .comparingInt(ContractRegistry::staticSegmentCount).reversed()
                        .thenComparingInt(ContractRegistry::variableSegmentCount)
                        .thenComparingInt(OperationContract::order))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown  operation: " + method + " " + normalized));
    }

    private static int staticSegmentCount(OperationContract operation) {
        int count = 0;
        for (String segment : segments(operation.path())) {
            if (!isVariableSegment(segment)) {
                count++;
            }
        }
        return count;
    }

    private static int variableSegmentCount(OperationContract operation) {
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
