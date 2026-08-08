package vip.isass.framework.web.security.authentication.apikey;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpringApiKeyHeaderProviderTest {

    @Test
    void onlyAddsApiKeyForConfiguredInternalEndpoint() {
        ServiceAccountProperties properties = new ServiceAccountProperties();
        properties.setApiKey("isass_sk_identifier_secret");
        SpringApiKeyHeaderProvider provider = new SpringApiKeyHeaderProvider(properties,
                new MockEnvironment().withProperty("isass.http.endpoints.bsp-service.url", "https://bsp.internal:31010"),
                new StaticListableBeanFactory());

        assertTrue(provider.support("POST", "https://bsp.internal:31010/bsp-service/init-data/import"));
        assertFalse(provider.support("POST", "https://storage.example/bucket/object"));
        assertFalse(provider.support("POST", "https://bsp.internal:31011/bsp-service/init-data/import"));
    }
}
