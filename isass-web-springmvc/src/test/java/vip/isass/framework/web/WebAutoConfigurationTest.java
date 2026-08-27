// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.webmvc.error.ErrorAttributes;
import vip.isass.framework.web.exception.ExceptionAdvice;
import vip.isass.framework.web.security.PermitUrlProvider;
import vip.isass.framework.web.security.EntrypointAnonymousUrlProvider;
import vip.isass.framework.web.servicedocs.ServiceDocsController;
import vip.isass.framework.entrypoint.registry.ServiceDefinitionRegistry;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(ErrorAttributes.class, () -> mock(ErrorAttributes.class))
            .withBean(ServiceDefinitionRegistry.class, () -> {
                ServiceDefinitionRegistry registry = mock(ServiceDefinitionRegistry.class);
                when(registry.all()).thenReturn(java.util.List.of());
                return registry;
            })
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withPropertyValues("spring.application.name=test-app")
            .withConfiguration(AutoConfigurations.of(WebAutoConfiguration.class));

    @Test
    void serviceDocsControllerIsCreated() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ServiceDocsController.class);
        });
    }

    @Test
    void openApiUrlsAreExposedAsPermitUrls() {
        contextRunner.run(context -> {
            assertThat(context.getBeansOfType(PermitUrlProvider.class).values())
                    .anySatisfy(provider -> assertThat(provider.getUrls())
                            .contains("/v3/api-docs", "/test-app/v3/api-docs"));
        });
    }

    @Test
    void entrypointAnonymousUrlProviderIsCreated() {
        contextRunner.run(context -> assertThat(context)
                .hasSingleBean(EntrypointAnonymousUrlProvider.class));
    }

    @Test
    void exceptionAdviceIsCreated() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ExceptionAdvice.class);
        });
    }
}
