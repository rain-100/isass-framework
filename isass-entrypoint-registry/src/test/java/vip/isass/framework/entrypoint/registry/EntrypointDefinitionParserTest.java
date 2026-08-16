// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.entrypoint.registry;

import org.junit.jupiter.api.Test;
import vip.isass.framework.entrypoint.IEntrypoint;
import vip.isass.framework.entrypoint.annotation.BodyParam;
import vip.isass.framework.entrypoint.annotation.EntrypointInfo;
import vip.isass.framework.entrypoint.annotation.EntrypointOperation;
import vip.isass.framework.entrypoint.metadata.HttpMethod;
import vip.isass.framework.entrypoint.metadata.OperationDefinition;
import vip.isass.framework.entrypoint.metadata.ServiceDefinition;

import java.lang.reflect.ParameterizedType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class EntrypointDefinitionParserTest {

    @Test
    void resolvesInheritedCrudGenericTypesAgainstConcreteService() {
        ServiceDefinition definition = new EntrypointDefinitionParser(
                java.util.List.of())
                .parse(TestCrudService.class, true);

        OperationDefinition superCud = definition.operations().stream()
                .filter(operation -> "superCud".equals(operation.operationName()))
                .findFirst()
                .orElseThrow();
        ParameterizedType requestType = assertInstanceOf(
                ParameterizedType.class, superCud.parameters().getFirst().javaType());
        assertEquals(TestRequest.class, requestType.getRawType());
        assertEquals(TestEntity.class, requestType.getActualTypeArguments()[0]);
        assertEquals(TestCriteria.class, requestType.getActualTypeArguments()[1]);

        ParameterizedType resultType = assertInstanceOf(ParameterizedType.class, superCud.returnType());
        assertEquals(TestResult.class, resultType.getRawType());
        assertEquals(TestEntity.class, resultType.getActualTypeArguments()[0]);
    }

    @EntrypointInfo(serviceName = "test-service", contextName = "sample", resourceName = "testEntity")
    private interface TestCrudService extends GenericEntrypoint<TestEntity, TestCriteria> {
    }

    private interface GenericEntrypoint<E, C> extends IEntrypoint {
        @EntrypointOperation(operationName = "superCud", displayName = "超级增删改",
                httpMethod = HttpMethod.POST)
        TestResult<E> superCud(@BodyParam TestRequest<E, C> request);
    }

    private record TestRequest<E, C>(E entity, C criteria) {}

    private record TestResult<E>(E entity) {}

    private static final class TestEntity {}

    private static final class TestCriteria {}
}
