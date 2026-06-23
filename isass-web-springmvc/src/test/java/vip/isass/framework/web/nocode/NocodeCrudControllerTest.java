package vip.isass.framework.web.nocode;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMethod;
import vip.isass.framework.nocode.v3.access.NocodeAccessHandler;
import vip.isass.framework.nocode.v3.operation.NocodeCrudOperation;
import vip.isass.framework.nocode.v3.operation.NocodeOperation;
import vip.isass.framework.nocode.v3.operation.NocodeOperationExecutor;
import vip.isass.framework.nocode.v3.routing.NocodeOperationProvider;
import vip.isass.framework.nocode.v3.routing.NocodeProviderType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NocodeCrudControllerTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(WebAutoConfigurationStub.class))
            .withUserConfiguration(TestNocodeConfig.class);

    @Test
    void nocodeCrudControllerIsRegistered() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(NocodeCrudController.class);
            assertThat(context).hasSingleBean(NocodeSpringMvcCrudEndpointInvoker.class);
            assertThat(context).hasSingleBean(NocodeSpringMvcQueryCriteriaParser.class);
        });
    }

    @Test
    void findByIdInvokesAccessHandler() {
        NocodeAccessHandler accessHandler = new NocodeAccessHandler(
                new NocodeOperationExecutor(List.of(new TestProvider()), List.of()));
        NocodeSpringMvcCrudEndpointInvoker invoker = new NocodeSpringMvcCrudEndpointInvoker(accessHandler);
        NocodeCrudController controller = new NocodeCrudController(
                invoker, new NocodeSpringMvcQueryCriteriaParser());

        Object result = controller.findById("testEntity", "42");

        assertThat(result).isEqualTo("testEntity:findById:42");
    }

    @Test
    void listInvokesAccessHandler() {
        NocodeAccessHandler accessHandler = new NocodeAccessHandler(
                new NocodeOperationExecutor(List.of(new TestProvider()), List.of()));
        NocodeSpringMvcCrudEndpointInvoker invoker = new NocodeSpringMvcCrudEndpointInvoker(accessHandler);
        NocodeCrudController controller = new NocodeCrudController(
                invoker, new NocodeSpringMvcQueryCriteriaParser());

        Object result = controller.list("testEntity", new org.springframework.util.LinkedMultiValueMap<>());

        assertThat(result).isEqualTo("testEntity:list:");
    }

    @Configuration
    static class TestNocodeConfig {

        @Bean
        public NocodeOperationExecutor nocodeOperationExecutor() {
            return new NocodeOperationExecutor(List.of(new TestProvider()), List.of());
        }

        @Bean
        public NocodeAccessHandler nocodeAccessHandler(NocodeOperationExecutor executor) {
            return new NocodeAccessHandler(executor);
        }

        @Bean
        public NocodeSpringMvcCrudEndpointInvoker nocodeSpringMvcCrudEndpointInvoker(
                NocodeAccessHandler accessHandler) {
            return new NocodeSpringMvcCrudEndpointInvoker(accessHandler);
        }

        @Bean
        public NocodeSpringMvcQueryCriteriaParser nocodeSpringMvcQueryCriteriaParser() {
            return new NocodeSpringMvcQueryCriteriaParser();
        }

        @Bean
        public NocodeCrudController nocodeCrudController(
                NocodeSpringMvcCrudEndpointInvoker invoker,
                NocodeSpringMvcQueryCriteriaParser parser) {
            return new NocodeCrudController(invoker, parser);
        }
    }

    static class TestProvider implements NocodeOperationProvider<String> {

        @Override
        public NocodeProviderType getProviderType() {
            return NocodeProviderType.LOCAL;
        }

        @Override
        public boolean supports(NocodeOperation operation) {
            return "testEntity".equals(operation.entityName());
        }

        @Override
        public String invoke(NocodeOperation operation) {
            return operation.entityName() + ":" + operation.operationName() + ":" +
                    operation.arguments().getOrDefault("id", "");
        }
    }

    /**
     * Minimal stub — the real WebAutoConfiguration requires too many deps for a unit test.
     */
    @Configuration
    static class WebAutoConfigurationStub {
    }
}
