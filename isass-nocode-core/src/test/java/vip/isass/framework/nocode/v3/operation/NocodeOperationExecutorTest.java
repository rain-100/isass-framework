package vip.isass.framework.nocode.v3.operation;

import org.junit.jupiter.api.Test;
import vip.isass.framework.nocode.v3.routing.NocodeOperationProvider;
import vip.isass.framework.nocode.v3.routing.NocodeProviderType;
import vip.isass.framework.nocode.v3.routing.NocodeRouteMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NocodeOperationExecutorTest {

    private final NocodeOperation operation = new NocodeOperation("attachment", "getById", Map.of("id", 1), String.class);

    @Test
    void routesProviderAndExecutesThroughPipeline() {
        List<String> events = new ArrayList<>();
        NocodeOperationExecutor executor = new NocodeOperationExecutor(
                List.of(
                        new TestProvider(NocodeProviderType.REMOTE, "remote"),
                        new TestProvider(NocodeProviderType.LOCAL, "local")
                ),
                List.of(new TestInterceptor(events))
        );

        String result = executor.execute(operation, NocodeRouteMode.AUTO);

        assertThat(result).isEqualTo("local");
        assertThat(events).containsExactly("before:getById", "after:getById");
    }

    @Test
    void explicitRouteModeCanSelectRemoteProvider() {
        NocodeOperationExecutor executor = new NocodeOperationExecutor(
                List.of(
                        new TestProvider(NocodeProviderType.LOCAL, "local"),
                        new TestProvider(NocodeProviderType.REMOTE, "remote")
                ),
                List.of()
        );

        String result = executor.execute(operation, NocodeRouteMode.REMOTE);

        assertThat(result).isEqualTo("remote");
    }

    record TestProvider(NocodeProviderType providerType, String result) implements NocodeOperationProvider<String> {

        @Override
        public NocodeProviderType getProviderType() {
            return providerType;
        }

        @Override
        public boolean supports(NocodeOperation operation) {
            return "attachment".equals(operation.entityName());
        }

        @Override
        public String invoke(NocodeOperation operation) {
            return result;
        }
    }

    record TestInterceptor(List<String> events) implements NocodeOperationInterceptor {

        @Override
        public <R> R intercept(NocodeOperation operation, NocodeOperationInvoker<R> next) {
            events.add("before:" + operation.operationName());
            R result = next.invoke(operation);
            events.add("after:" + operation.operationName());
            return result;
        }
    }
}
