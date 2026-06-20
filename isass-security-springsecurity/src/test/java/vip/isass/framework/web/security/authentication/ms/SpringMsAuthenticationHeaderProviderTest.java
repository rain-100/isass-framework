package vip.isass.framework.web.security.authentication.ms;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import vip.isass.framework.common.web.header.AdditionalRequestHeaderProvider;
import vip.isass.framework.common.web.security.authentication.ms.MsAuthenticationHeaderProvider;

import static org.assertj.core.api.Assertions.assertThat;

class SpringMsAuthenticationHeaderProviderTest {

    @Test
    void registersMicroserviceHeaderProviderOnlyInSecurityModule() {
        new ApplicationContextRunner()
                .withBean(SpringMsAuthenticationHeaderProvider.class)
                .withPropertyValues(
                        "spring.application.name=attachment-service",
                        "security.ms.secret=test-secret")
                .run(context -> {
                    assertThat(context).hasSingleBean(MsAuthenticationHeaderProvider.class);
                    assertThat(context).hasSingleBean(AdditionalRequestHeaderProvider.class);

                    MsAuthenticationHeaderProvider provider = context.getBean(MsAuthenticationHeaderProvider.class);
                    assertThat(provider.getHeaderName()).isEqualTo(MsAuthenticationHeaderProvider.HEADER);
                    assertThat(provider.getSecret()).isEqualTo("test-secret");
                    assertThat(provider.getDotSecret()).isEqualTo(".test-secret");
                    assertThat(provider.getValue()).isEqualTo("attachment-service.test-secret");
                });
    }
}
