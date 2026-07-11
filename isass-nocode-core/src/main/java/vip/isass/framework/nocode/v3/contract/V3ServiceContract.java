package vip.isass.framework.nocode.v3.contract;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record V3ServiceContract(
        String serviceName,
        String entityName,
        String serviceInterface,
        String entityJavaType,
        String criteriaJavaType,
        String tag,
        List<V3OperationContract> operations
) {
    public V3ServiceContract {
        serviceName = requireText(serviceName, "serviceName");
        entityName = requireText(entityName, "entityName");
        serviceInterface = requireText(serviceInterface, "serviceInterface");
        entityJavaType = requireText(entityJavaType, "entityJavaType");
        criteriaJavaType = requireText(criteriaJavaType, "criteriaJavaType");
        tag = tag == null || tag.isBlank() ? serviceInterface : tag;
        operations = List.copyOf(operations == null ? List.of() : operations);
        validateUniqueOperations(operations);
    }

    public V3ServiceContract(String serviceName, String entityName, String serviceInterface,
                             String entityJavaType, String criteriaJavaType,
                             List<V3OperationContract> operations) {
        this(serviceName, entityName, serviceInterface, entityJavaType, criteriaJavaType,
                serviceInterface, operations);
    }

    private static void validateUniqueOperations(List<V3OperationContract> operations) {
        Set<String> names = new HashSet<>();
        Set<String> routes = new HashSet<>();
        for (V3OperationContract operation : operations) {
            if (!names.add(operation.name())) {
                throw new IllegalArgumentException("Overloaded operation is not supported: " + operation.name());
            }
            String route = operation.httpMethod() + " " + operation.path();
            if (!routes.add(route)) {
                throw new IllegalArgumentException("Duplicate HTTP route: " + route);
            }
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
