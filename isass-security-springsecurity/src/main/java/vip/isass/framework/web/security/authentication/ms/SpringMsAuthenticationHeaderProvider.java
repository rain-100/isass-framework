package vip.isass.framework.web.security.authentication.ms;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import vip.isass.framework.common.web.security.authentication.ms.MsAuthenticationHeaderProvider;

/**
 * Spring Boot backed microservice authentication request header provider.
 *
 * @author Rain
 */
@Component
public class SpringMsAuthenticationHeaderProvider extends MsAuthenticationHeaderProvider {

    @Override
    @Value("${spring.application.name:unknown}")
    public MsAuthenticationHeaderProvider setAppName(String appName) {
        return super.setAppName(appName);
    }

    @Override
    @Value("${security.ms.secret:qcyAHr35IDzI9FkD}")
    public MsAuthenticationHeaderProvider setSecret(String secret) {
        super.setSecret(secret);
        super.setDotSecret("." + secret);
        return this;
    }
}
