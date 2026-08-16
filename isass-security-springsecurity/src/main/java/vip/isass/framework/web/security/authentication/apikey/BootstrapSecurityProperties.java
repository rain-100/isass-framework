// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.authentication.apikey;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 当前微服务用于首次 Bootstrap 注册和后续跨服务调用的安全配置。
 *
 * <p>HMAC 凭证只用于签名 Bootstrap 注册请求；API Key 在首次注册时创建默认服务账号凭证，
 * 注册完成后继续作为当前微服务主动调用其他服务的应用凭证。</p>
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "isass.security.bootstrap")
public class BootstrapSecurityProperties {

    private String hmacKeyId;
    private String hmacSecret;
    private String apiKey;

    public boolean apiKeyEnabled() {
        return StrUtil.isNotBlank(apiKey);
    }
}
