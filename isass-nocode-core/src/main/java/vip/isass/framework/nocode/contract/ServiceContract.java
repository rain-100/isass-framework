package vip.isass.framework.nocode.contract;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record ServiceContract(
        String serviceName,
        String entityName,
        String serviceInterface,
        String entityJavaType,
        String criteriaJavaType,
        String tag,
        List<OperationContract> operations
) {
    public ServiceContract {
        serviceName = requireText(serviceName, "serviceName");
        entityName = requireText(entityName, "entityName");
        serviceInterface = requireText(serviceInterface, "serviceInterface");
        entityJavaType = requireText(entityJavaType, "entityJavaType");
        criteriaJavaType = requireText(criteriaJavaType, "criteriaJavaType");
        tag = tag == null || tag.isBlank() ? serviceInterface : tag;
        operations = List.copyOf(operations == null ? List.of() : operations);
        validateUniqueOperations(operations);
    }

    public ServiceContract(String serviceName, String entityName, String serviceInterface,
                             String entityJavaType, String criteriaJavaType,
                             List<OperationContract> operations) {
        this(serviceName, entityName, serviceInterface, entityJavaType, criteriaJavaType,
                serviceInterface, operations);
    }

    private static void validateUniqueOperations(List<OperationContract> operations) {
        Set<String> names = new HashSet<>();
        Set<String> routes = new HashSet<>();
        for (OperationContract operation : operations) {
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
