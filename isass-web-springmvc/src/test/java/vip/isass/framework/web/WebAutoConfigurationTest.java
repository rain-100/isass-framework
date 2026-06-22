package vip.isass.framework.web;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.webmvc.error.ErrorAttributes;
import vip.isass.framework.web.exception.ExceptionAdvice;
import vip.isass.framework.web.servicedocs.ServiceDocsScanner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class WebAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(ErrorAttributes.class, () -> mock(ErrorAttributes.class))
            .withPropertyValues("spring.application.name=test-app")
            .withConfiguration(AutoConfigurations.of(WebAutoConfiguration.class));

    @Test
    void serviceDocsScannerIsCreated() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ServiceDocsScanner.class);
        });
    }

    @Test
    void exceptionAdviceIsCreated() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ExceptionAdvice.class);
        });
    }
}
