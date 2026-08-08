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
            String key = key(contract.service(), contract.entity());
            if (indexed.putIfAbsent(key, contract) != null) {
                throw new IllegalArgumentException("Duplicate  service contract: " + key);
            }
        }
        this.services = Map.copyOf(indexed);
    }

    public ServiceContract requireService(String service, String entity) {
        ServiceContract contract = services.get(key(service, entity));
        if (contract == null) {
            throw new IllegalArgumentException("Unknown  service: " + service + "/" + entity);
        }
        return contract;
    }

    public List<ServiceContract> contracts() {
        return List.copyOf(services.values());
    }

    /** Resolves an entity name to its owning microservice for portable initialization documents. */
    public ServiceContract requireServiceByEntity(String entity) {
        List<ServiceContract> matches = services.values().stream()
                .filter(contract -> contract.entity().equals(entity))
                .toList();
        if (matches.isEmpty()) {
            throw new IllegalArgumentException("Unknown nocode entity: " + entity);
        }
        if (matches.size() > 1) {
            throw new IllegalArgumentException("Ambiguous nocode entity: " + entity);
        }
        return matches.getFirst();
    }

    public OperationContract requireOperation(
            String service,
            String entity,
            HttpMethod method,
            String relativePath
    ) {
        String normalized = relativePath.startsWith("/") ? relativePath : "/" + relativePath;
        return requireService(service, entity).operations().stream()
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

    private String key(String service, String entity) {
        return service + '\n' + entity;
    }
}
