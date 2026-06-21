package vip.isass.framework.nocode.v3.access;

import org.junit.jupiter.api.Test;
import vip.isass.framework.nocode.v3.operation.NocodeOperation;
import vip.isass.framework.nocode.v3.operation.NocodeOperationExecutor;
import vip.isass.framework.nocode.v3.routing.NocodeOperationProvider;
import vip.isass.framework.nocode.v3.routing.NocodeProviderType;
import vip.isass.framework.nocode.v3.routing.NocodeRouteMode;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NocodeAccessHandlerTest {

    @Test
    void convertsAccessRequestToOperationAndExecutesIt() {
        NocodeOperationExecutor executor = new NocodeOperationExecutor(
                List.of(new TestProvider()),
                List.of()
        );
        NocodeAccessHandler handler = new NocodeAccessHandler(executor);
        NocodeAccessRequest request = new NocodeAccessRequest(
                "attachment",
                "getById",
                Map.of("id", 1),
                String.class,
                NocodeRouteMode.LOCAL
        );

        String result = handler.handle(request);

        assertThat(result).isEqualTo("attachment:1");
    }

    @Test
    void validatesStandardCrudRequestBeforeExecution() {
        NocodeOperationExecutor executor = new NocodeOperationExecutor(
                List.of(new TestProvider()),
                List.of()
        );
        NocodeAccessHandler handler = new NocodeAccessHandler(executor);
        NocodeAccessRequest request = new NocodeAccessRequest(
                "attachment",
                "findById",
                Map.of(),
                String.class,
                NocodeRouteMode.LOCAL
        );

        assertThatThrownBy(() -> handler.handle(request))
                .isInstanceOf(NocodeAccessValidationException.class)
                .hasMessageContaining("Missing required argument 'id'");
    }

    static class TestProvider implements NocodeOperationProvider<String> {

        @Override
        public NocodeProviderType getProviderType() {
            return NocodeProviderType.LOCAL;
        }

        @Override
        public boolean supports(NocodeOperation operation) {
            return "attachment".equals(operation.entityName())
                    && "getById".equals(operation.operationName());
        }

        @Override
        public String invoke(NocodeOperation operation) {
            return operation.entityName() + ":" + operation.arguments().get("id");
        }
    }
}
