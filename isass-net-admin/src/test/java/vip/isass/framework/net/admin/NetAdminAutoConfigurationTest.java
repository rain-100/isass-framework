package vip.isass.framework.net.admin;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import vip.isass.framework.net.admin.controller.NetAdminController;
import vip.isass.framework.net.core.session.ISessionService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class NetAdminAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(ISessionService.class, () -> mock(ISessionService.class))
            .withConfiguration(AutoConfigurations.of(NetAdminAutoConfiguration.class));

    @Test
    void netAdminControllerIsCreated() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(NetAdminController.class);
        });
    }

    @Test
    void netAdminControllerCanBeOverridden() {
        contextRunner
                .withBean("customNetAdminController", NetAdminController.class,
                        () -> new NetAdminController(mock(ISessionService.class)))
                .run(context -> {
                    assertThat(context).hasSingleBean(NetAdminController.class);
                    assertThat(context.getBean("customNetAdminController"))
                            .isInstanceOf(NetAdminController.class);
                });
    }
}
