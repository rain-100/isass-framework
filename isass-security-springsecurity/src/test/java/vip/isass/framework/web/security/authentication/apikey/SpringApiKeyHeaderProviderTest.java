// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.authentication.apikey;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpringApiKeyHeaderProviderTest {

    @Test
    void onlyAddsApiKeyForConfiguredInternalEndpoint() {
        BootstrapSecurityProperties properties = new BootstrapSecurityProperties();
        properties.setApiKey("isass_sk_identifier_secret");
        SpringApiKeyHeaderProvider provider = new SpringApiKeyHeaderProvider(properties,
                new MockEnvironment().withProperty(
                        "isass.entrypoint.http.services.bsp-service.url", "https://bsp.internal:31010"),
                new StaticListableBeanFactory());

        assertTrue(provider.support("POST", "https://bsp.internal:31010/bsp-service/nocode/system/initialization/importData"));
        assertTrue(provider.support("GET", "https://bsp.internal:31010/bsp-service/auth/bootstrap/diagnostics"));
        assertFalse(provider.support("POST", "https://bsp.internal:31010/bsp-service/auth/bootstrap/apiKey"));
        assertFalse(provider.support("POST", "https://bsp.internal:31010/bsp-service/auth/bootstrap/register"));
        assertFalse(provider.support("POST", "https://storage.example/bucket/object"));
        assertFalse(provider.support("POST", "https://bsp.internal:31011/bsp-service/nocode/system/initialization/importData"));
    }

    @Test
    void addsApiKeyForEntrypointBaseUrl() {
        BootstrapSecurityProperties properties = new BootstrapSecurityProperties();
        properties.setApiKey("isass_sk_identifier_secret");
        SpringApiKeyHeaderProvider provider = new SpringApiKeyHeaderProvider(properties,
                new MockEnvironment().withProperty("isass.entrypoint.http.base-url", "https://gateway.internal:31010"),
                new StaticListableBeanFactory());

        assertTrue(provider.support("GET", "https://gateway.internal:31010/bsp-service/config/parameter/getCodeValuesByKey"));
        assertFalse(provider.support("GET", "https://gateway.internal:31011/bsp-service/config/parameter/getCodeValuesByKey"));
    }

    @Test
    void readsApiKeyFromBootstrapSecurityConfiguration() {
        ConfigurationProperties annotation = BootstrapSecurityProperties.class
                .getAnnotation(ConfigurationProperties.class);

        assertEquals("isass.security.bootstrap", annotation.prefix());
    }
}
