// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import vip.isass.framework.entrypoint.authorization.UrlAccessSecurityStrategy;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/** Security settings shared by web security and metadata resolution. */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "isass.security")
public class SecurityProperties {

    private UrlAccessSecurityStrategy urlAccessSecurityStrategy = UrlAccessSecurityStrategy.NONE;

    private Rsa rsa = new Rsa();

    private Internal internal = new Internal();

    @Getter
    @Setter
    public static class Rsa {

        /** Base64-encoded X.509 RSA public key. */
        private String publicKey;

        /** Base64-encoded PKCS#8 RSA private key. */
        private String privateKey;
    }

    /** 内部微服务 HMAC 认证配置；入口范围由 Java InternalAccessProvider 定义。 */
    @Getter
    @Setter
    public static class Internal {

        private String hmacKeyId;

        private String hmacSecret;

        private Duration allowedClockSkew = Duration.ofMinutes(5);

        private Map<String, String> trustedKeys = new LinkedHashMap<>();

        public boolean enabled() {
            return hmacKeyId != null && !hmacKeyId.isBlank()
                    && hmacSecret != null && !hmacSecret.isBlank();
        }

        public Map<String, String> verificationKeys() {
            LinkedHashMap<String, String> result = new LinkedHashMap<>(trustedKeys);
            if (enabled()) {
                result.putIfAbsent(hmacKeyId, hmacSecret);
            }
            return Map.copyOf(result);
        }
    }
}
