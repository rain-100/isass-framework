package vip.isass.framework.nocode.v3.operation;

import org.junit.jupiter.api.Test;
import vip.isass.framework.common.support.api.IsassOrder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NocodeOperationListenerInterceptorTest {

    private final NocodeOperation operation = new NocodeOperation("attachment", "save", Map.of("id", 1), String.class);

    @Test
    void invokesListenersAroundSuccessfulOperation() {
        List<String> events = new ArrayList<>();
        NocodeOperationListenerInterceptor interceptor = new NocodeOperationListenerInterceptor(List.of(
                new SecondListener(events),
                new FirstListener(events)
        ));

        String result = interceptor.intercept(operation, currentOperation -> {
            events.add("invoke:" + currentOperation.operationName());
            return "ok";
        });

        assertThat(result).isEqualTo("ok");
        assertThat(events).containsExactly(
                "first-before:save",
                "second-before:save",
                "invoke:save",
                "second-after:ok",
                "first-after:ok"
        );
    }

    @Test
    void invokesErrorListenersAndRethrowsException() {
        List<String> events = new ArrayList<>();
        NocodeOperationListenerInterceptor interceptor = new NocodeOperationListenerInterceptor(List.of(
                new SecondListener(events),
                new FirstListener(events)
        ));

        assertThatThrownBy(() -> interceptor.intercept(operation, currentOperation -> {
            events.add("invoke:" + currentOperation.operationName());
            throw new IllegalStateException("boom");
        })).isInstanceOf(IllegalStateException.class)
                .hasMessage("boom");

        assertThat(events).containsExactly(
                "first-before:save",
                "second-before:save",
                "invoke:save",
                "second-error:boom",
                "first-error:boom"
        );
    }

    @IsassOrder(1)
    record FirstListener(List<String> events) implements NocodeOperationListener {

        @Override
        public void before(NocodeOperation operation) {
            events.add("first-before:" + operation.operationName());
        }

        @Override
        public void after(NocodeOperation operation, Object result) {
            events.add("first-after:" + result);
        }

        @Override
        public void onError(NocodeOperation operation, RuntimeException exception) {
            events.add("first-error:" + exception.getMessage());
        }
    }

    @IsassOrder(2)
    record SecondListener(List<String> events) implements NocodeOperationListener {

        @Override
        public void before(NocodeOperation operation) {
            events.add("second-before:" + operation.operationName());
        }

        @Override
        public void after(NocodeOperation operation, Object result) {
            events.add("second-after:" + result);
        }

        @Override
        public void onError(NocodeOperation operation, RuntimeException exception) {
            events.add("second-error:" + exception.getMessage());
        }
    }
}
