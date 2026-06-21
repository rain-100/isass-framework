package vip.isass.framework.nocode.v3.operation;

import org.junit.jupiter.api.Test;
import vip.isass.framework.common.support.api.IsassOrder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NocodeOperationPipelineTest {

    @Test
    void invokesInterceptorsByOrderAroundFinalInvoker() {
        List<String> events = new ArrayList<>();
        NocodeOperation operation = new NocodeOperation("attachment", "findPage", Map.of("page", 1), String.class);
        NocodeOperationPipeline pipeline = new NocodeOperationPipeline(List.of(
                new SecondInterceptor(events),
                new FirstInterceptor(events)
        ));

        String result = pipeline.invoke(operation, currentOperation -> {
            events.add("invoke:" + currentOperation.operationName());
            return "result";
        });

        assertThat(result).isEqualTo("result");
        assertThat(events).containsExactly(
                "first-before",
                "second-before",
                "invoke:findPage",
                "second-after",
                "first-after"
        );
    }

    @IsassOrder(1)
    static class FirstInterceptor implements NocodeOperationInterceptor {

        private final List<String> events;

        FirstInterceptor(List<String> events) {
            this.events = events;
        }

        @Override
        public <R> R intercept(NocodeOperation operation, NocodeOperationInvoker<R> next) {
            events.add("first-before");
            R result = next.invoke(operation);
            events.add("first-after");
            return result;
        }
    }

    @IsassOrder(2)
    static class SecondInterceptor implements NocodeOperationInterceptor {

        private final List<String> events;

        SecondInterceptor(List<String> events) {
            this.events = events;
        }

        @Override
        public <R> R intercept(NocodeOperation operation, NocodeOperationInvoker<R> next) {
            events.add("second-before");
            R result = next.invoke(operation);
            events.add("second-after");
            return result;
        }
    }
}
