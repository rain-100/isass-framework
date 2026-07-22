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

    private Rsa rsa = new Rsa();

    @Getter
    @Setter
    public static class Rsa {

        /** Base64-encoded X.509 RSA public key. */
        private String publicKey;

        /** Base64-encoded PKCS#8 RSA private key. */
        private String privateKey;
    }
}
