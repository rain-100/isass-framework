package vip.isass.framework.nocode.v3.routing;

import org.junit.jupiter.api.Test;
import vip.isass.framework.nocode.v3.operation.NocodeOperation;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NocodeOperationRouterTest {

    private final NocodeOperation operation = new NocodeOperation("attachment", "getById", Map.of("id", "1"), String.class);

    @Test
    void autoRoutePrefersLocalProvider() {
        NocodeOperationRouter router = new NocodeOperationRouter(List.of(
                new TestProvider(NocodeProviderType.REMOTE, "remote"),
                new TestProvider(NocodeProviderType.LOCAL, "local")
        ));

        NocodeOperationProvider<String> provider = router.route(operation, NocodeRouteMode.AUTO);

        assertThat(provider.invoke(operation)).isEqualTo("local");
    }

    @Test
    void explicitRemoteRouteUsesRemoteProvider() {
        NocodeOperationRouter router = new NocodeOperationRouter(List.of(
                new TestProvider(NocodeProviderType.LOCAL, "local"),
                new TestProvider(NocodeProviderType.REMOTE, "remote")
        ));

        NocodeOperationProvider<String> provider = router.route(operation, NocodeRouteMode.REMOTE);

        assertThat(provider.invoke(operation)).isEqualTo("remote");
    }

    @Test
    void throwsWhenNoProviderSupportsOperation() {
        NocodeOperationRouter router = new NocodeOperationRouter(List.of());

        assertThatThrownBy(() -> router.route(operation, NocodeRouteMode.AUTO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("attachment.getById");
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
}
