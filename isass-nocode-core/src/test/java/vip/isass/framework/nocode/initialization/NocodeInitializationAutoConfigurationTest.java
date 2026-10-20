// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.initialization;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import vip.isass.framework.entrypoint.http.HttpEndpointResolver;
import vip.isass.framework.entrypoint.registry.ServiceDefinitionRegistry;
import vip.isass.framework.nocode.NocodeAutoConfiguration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NocodeInitializationAutoConfigurationTest {

    @Test
    void wiresOneSharedImporterForTheControllerAndStartupRunner() {
        var registry = mock(ServiceDefinitionRegistry.class);
        when(registry.all()).thenReturn(List.of());

        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(NocodeAutoConfiguration.class))
                .withBean(ServiceDefinitionRegistry.class, () -> registry)
                .withBean(HttpEndpointResolver.class, () -> ignored -> null)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(NocodeInitializationImporter.class);
                    assertThat(context).hasSingleBean(NocodeInitializationController.class);
                    assertThat(context).hasBean("nocodeInitializationRunner");
                    assertThat(context.getBean(NocodeInitializationDataService.class))
                            .isInstanceOf(INocodeInitializationService.class);
                    assertThat(context.getBean(NocodeInitializationRemoteClient.class))
                            .isInstanceOf(INocodeInitializationService.class);
                });
    }
}
