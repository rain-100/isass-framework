package vip.isass.framework.web.security.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Security settings shared by web security and metadata resolution. */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {

    private UrlAccessSecurityStrategy urlAccessSecurityStrategy = UrlAccessSecurityStrategy.NONE;
}
