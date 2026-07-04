package vip.isass.framework.nocode.v3.transport;

import org.junit.jupiter.api.Test;
import vip.isass.framework.nocode.v3.contract.V3HttpMethod;
import vip.isass.framework.nocode.v3.contract.V3OperationContract;
import vip.isass.framework.nocode.v3.contract.V3ParameterContract;
import vip.isass.framework.nocode.v3.contract.V3ParameterSource;
import vip.isass.framework.nocode.v3.contract.V3ServiceContract;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class V3ServiceProxyFactoryTest {

    @Test
    void mapsInterfaceMethodToLogicalInvocation() {
        V3OperationContract operation = new V3OperationContract(
                "findName", V3HttpMethod.GET, "/name/{id}", 301, true,
                List.of(new V3ParameterContract(
                        "id", "java.lang.Long", V3ParameterSource.PATH, true, "主键")),
                "java.lang.String", "查询名称");
        V3ServiceContract contract = new V3ServiceContract(
                "asset-service", "icon", ExampleService.class.getName(),
                "sample.Icon", "sample.IconCriteria", List.of(operation));
        V3InvocationTransport transport = new V3InvocationTransport() {
            public V3TransportKind kind() { return V3TransportKind.GRPC; }
            public boolean available(V3Invocation invocation) { return true; }
            public Object invoke(V3Invocation invocation) {
                return invocation.serviceName() + ":" + invocation.entityName() + ":"
                        + invocation.operationName() + ":" + invocation.arguments().getFirst();
            }
        };

        ExampleService service = new V3ServiceProxyFactory(new V3TransportResolver())
                .create(ExampleService.class, contract, List.of(transport));

        assertEquals("asset-service:icon:findName:9", service.findName(9L));
    }

    interface ExampleService {
        String findName(Long id);
    }
}
