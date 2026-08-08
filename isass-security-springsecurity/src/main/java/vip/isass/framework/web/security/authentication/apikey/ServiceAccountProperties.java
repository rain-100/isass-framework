package vip.isass.framework.web.security.authentication.apikey;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 固定配置当前微服务用于主动跨服务调用的服务账号。
 *
 * <p>服务账号、租户和应用均由 BSP 管理员预先创建；应用启动时不会创建或修改这些资源。
 * API Key 是唯一需要部署到调用方的凭证，目标服务会从其对应的服务账号解析租户和应用上下文。</p>
 */
@Getter
@Setter
@Component
@ConfigurationProperties("isass.security.service-account")
public class ServiceAccountProperties {

    private String apiKey;

    public boolean enabled() {
        return StrUtil.isNotBlank(apiKey);
    }
}
