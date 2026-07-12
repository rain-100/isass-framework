package vip.isass.framework.nocode.transport;

import org.junit.jupiter.api.Test;
import vip.isass.framework.nocode.contract.HttpMethod;
import vip.isass.framework.nocode.contract.OperationContract;
import vip.isass.framework.nocode.contract.ParameterContract;
import vip.isass.framework.nocode.contract.ParameterSource;
import vip.isass.framework.nocode.contract.ServiceContract;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServiceProxyFactoryTest {

    @Test
    void mapsInterfaceMethodToLogicalInvocation() {
        OperationContract operation = new OperationContract(
                "findName", HttpMethod.GET, "/name/{id}", 301, true,
                List.of(new ParameterContract(
                        "id", "java.lang.Long", ParameterSource.PATH, true, "主键")),
                "java.lang.String", "查询名称");
        ServiceContract contract = new ServiceContract(
                "asset-service", "icon", ExampleService.class.getName(),
                "sample.Icon", "sample.IconCriteria", List.of(operation));
        InvocationTransport transport = new InvocationTransport() {
            public TransportKind kind() { return TransportKind.GRPC; }
            public boolean available(Invocation invocation) { return true; }
            public Object invoke(Invocation invocation) {
                return invocation.serviceName() + ":" + invocation.entityName() + ":"
                        + invocation.operationName() + ":" + invocation.arguments().getFirst();
            }
        };

        ExampleService service = new ServiceProxyFactory(new TransportResolver())
                .create(ExampleService.class, contract, List.of(transport));

        assertEquals("asset-service:icon:findName:9", service.findName(9L));
    }

    interface ExampleService {
        String findName(Long id);
    }
}
