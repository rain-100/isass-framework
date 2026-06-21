package vip.isass.framework.web.nocode;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMethod;
import vip.isass.framework.nocode.v3.access.NocodeAccessHandler;
import vip.isass.framework.nocode.v3.operation.NocodeCrudOperation;
import vip.isass.framework.nocode.v3.operation.NocodeOperation;
import vip.isass.framework.nocode.v3.operation.NocodeOperationExecutor;
import vip.isass.framework.nocode.v3.routing.NocodeOperationProvider;
import vip.isass.framework.nocode.v3.routing.NocodeProviderType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NocodeSpringMvcCrudEndpointInvokerTest {

    @Test
    void invokesAccessHandlerWithRouteAndArguments() {
        NocodeAccessHandler accessHandler = new NocodeAccessHandler(
                new NocodeOperationExecutor(List.of(new TestProvider()), List.of())
        );
        NocodeSpringMvcCrudEndpointInvoker invoker = new NocodeSpringMvcCrudEndpointInvoker(accessHandler);
        NocodeSpringMvcCrudRoute route = new NocodeSpringMvcCrudRoute(
                NocodeCrudOperation.FIND_BY_ID,
                RequestMethod.GET,
                "/nocode/{entityName}/{id}",
                List.of("entityName", "id")
        );

        String result = invoker.invoke(route, NocodeSpringMvcCrudRequestArguments.byId("attachment", 1001L));

        assertThat(result).isEqualTo("attachment:findById:1001");
    }

    static class TestProvider implements NocodeOperationProvider<String> {

        @Override
        public NocodeProviderType getProviderType() {
            return NocodeProviderType.LOCAL;
        }

        @Override
        public boolean supports(NocodeOperation operation) {
            return "attachment".equals(operation.entityName())
                    && NocodeCrudOperation.FIND_BY_ID.getOperationName().equals(operation.operationName());
        }

        @Override
        public String invoke(NocodeOperation operation) {
            return operation.entityName() + ":" + operation.operationName() + ":" + operation.arguments().get("id");
        }
    }
}
