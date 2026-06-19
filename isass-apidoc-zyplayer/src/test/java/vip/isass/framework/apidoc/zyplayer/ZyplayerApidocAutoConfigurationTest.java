package vip.isass.framework.apidoc.zyplayer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import vip.isass.framework.apidoc.zyplayer.client.ZyplayerClientOperations;
import vip.isass.framework.apidoc.zyplayer.sync.ZyplayerDocSyncService;

import static org.assertj.core.api.Assertions.assertThat;

class ZyplayerApidocAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(ZyplayerApidocAutoConfiguration.class);

    @Test
    void doesNotCreateBeansWhenDisabled() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(ZyplayerDocSyncService.class));
    }

    @Test
    void createsSyncBeansWhenEnabledAndConfigured() {
        contextRunner
                .withPropertyValues(
                        "isass.apidoc.zyplayer.enabled=true",
                        "isass.apidoc.zyplayer.base-url=http://127.0.0.1:8083",
                        "isass.apidoc.zyplayer.api-key=test",
                        "isass.apidoc.zyplayer.private-key=MIIB",
                        "spring.application.name=attachment-service",
                        "info.service-name-cn=附件微服务")
                .withBean(ZyplayerClientOperations.class, FakeZyplayerClient::new)
                .run(context -> {
                    assertThat(context).hasSingleBean(ZyplayerApidocProperties.class);
                    assertThat(context).hasSingleBean(ZyplayerDocSyncService.class);
                    assertThat(context).hasSingleBean(ZyplayerServiceDescriptor.class);
                    assertThat(context.getBean(ZyplayerServiceDescriptor.class).spaceName()).isEqualTo("附件微服务v4.0.0");
                });
    }

    static class FakeZyplayerClient implements ZyplayerClientOperations {
        @Override
        public java.util.List<vip.isass.framework.apidoc.zyplayer.client.ZyplayerSpace> listSpaces() {
            return java.util.List.of();
        }

        @Override
        public vip.isass.framework.apidoc.zyplayer.client.ZyplayerSpace updateSpace(java.util.Map<String, Object> payload) {
            return null;
        }

        @Override
        public java.util.List<vip.isass.framework.apidoc.zyplayer.client.ZyplayerPage> listPages(Long spaceId) {
            return java.util.List.of();
        }

        @Override
        public vip.isass.framework.apidoc.zyplayer.client.ZyplayerPageContent pageDetail(Long spaceId, Long pageId) {
            return null;
        }

        @Override
        public vip.isass.framework.apidoc.zyplayer.client.ZyplayerPage updatePage(java.util.Map<String, Object> payload) {
            return null;
        }

        @Override
        public void deletePage(Long spaceId, Long pageId) {
        }

        @Override
        public void releasePage(Long pageId) {
        }
    }
}
