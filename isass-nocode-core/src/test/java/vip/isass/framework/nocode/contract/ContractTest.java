package vip.isass.framework.nocode.contract;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ContractTest {

    @Test
    void operationRejectsMissingPathParameter() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new OperationContract(
                        "findByTenant",
                        HttpMethod.GET,
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
        OperationContract first = operation("findAvailable", "/available");
        OperationContract second = operation("findAvailable", "/available2");

        assertThrows(IllegalArgumentException.class, () -> new ServiceContract(
                "attachment-service",
                "icon",
                "vip.isass.attachment.api.IIconService",
                "vip.isass.attachment.api.Icon",
                "vip.isass.attachment.api.IconCriteria",
                List.of(first, second)
        ));
    }

    @Test
    void standardFactoryBuildsAllOperationsInAddUpdateQueryDeleteOrder() {
        List<OperationContract> operations = StandardContractFactory.operations(
                "example.Icon", "example.IconCriteria");

        assertEquals(36, operations.size());
        assertEquals("add", operations.getFirst().name());
        assertEquals("updateById", operations.get(10).name());
        assertEquals("getById", operations.get(16).name());
        assertEquals("deleteById", operations.get(33).name());
    }

    @Test
    void registryPrefersStaticRouteOverEarlierPathVariableRoute() {
        ContractRegistry registry = new ContractRegistry(List.of(new ServiceContract(
                "attachment-service",
                "iconGroup",
                "example.IIconGroupService",
                "example.IconGroup",
                "example.IconGroupCriteria",
                StandardContractFactory.operations("example.IconGroup", "example.IconGroupCriteria")
        )));

        OperationContract operation = registry.requireOperation(
                "attachment-service", "iconGroup", HttpMethod.GET, "/criteria");

        assertEquals("findByCriteria", operation.name());
    }

    private OperationContract operation(String name, String path) {
        return new OperationContract(
                name, HttpMethod.GET, path, 501, true,
                List.of(), "java.lang.Boolean", name);
    }
}
