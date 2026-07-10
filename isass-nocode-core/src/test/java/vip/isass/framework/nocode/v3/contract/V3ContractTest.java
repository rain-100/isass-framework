package vip.isass.framework.nocode.v3.contract;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class V3ContractTest {

    @Test
    void operationRejectsMissingPathParameter() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new V3OperationContract(
                        "findByTenant",
                        V3HttpMethod.GET,
                        "/available/{tenantId}",
                        501,
                        true,
                        List.of(),
                        "java.util.List<Icon>",
                        "查询可用图标"
                ));

        assertEquals("Path variable 'tenantId' has no matching parameter in findByTenant",
                exception.getMessage());
    }

    @Test
    void serviceRejectsOverloadedOperationNames() {
        V3OperationContract first = operation("findAvailable", "/available");
        V3OperationContract second = operation("findAvailable", "/available2");

        assertThrows(IllegalArgumentException.class, () -> new V3ServiceContract(
                "attachment-service",
                "icon",
                "vip.isass.attachment.api.IV3IconService",
                "vip.isass.attachment.api.V3Icon",
                "vip.isass.attachment.api.V3IconCriteria",
                List.of(first, second)
        ));
    }

    @Test
    void standardFactoryBuildsAllOperationsInAddUpdateQueryDeleteOrder() {
        List<V3OperationContract> operations = V3StandardContractFactory.operations(
                "example.V3Icon", "example.V3IconCriteria");

        assertEquals(36, operations.size());
        assertEquals("add", operations.getFirst().name());
        assertEquals("updateById", operations.get(10).name());
        assertEquals("getById", operations.get(16).name());
        assertEquals("deleteById", operations.get(33).name());
    }

    @Test
    void registryPrefersStaticRouteOverEarlierPathVariableRoute() {
        V3ContractRegistry registry = new V3ContractRegistry(List.of(new V3ServiceContract(
                "attachment-service",
                "iconGroup",
                "example.IV3IconGroupService",
                "example.V3IconGroup",
                "example.V3IconGroupCriteria",
                V3StandardContractFactory.operations("example.V3IconGroup", "example.V3IconGroupCriteria")
        )));

        V3OperationContract operation = registry.requireOperation(
                "attachment-service", "iconGroup", V3HttpMethod.GET, "/criteria");

        assertEquals("findByCriteria", operation.name());
    }

    private V3OperationContract operation(String name, String path) {
        return new V3OperationContract(
                name, V3HttpMethod.GET, path, 501, true,
                List.of(), "java.lang.Boolean", name);
    }
}
