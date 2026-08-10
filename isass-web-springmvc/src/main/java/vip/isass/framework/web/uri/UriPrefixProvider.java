// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.uri;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

/**
 * @author Rain
 */

@Getter
public class UriPrefixProvider {
    private static final Logger log = LoggerFactory.getLogger(UriPrefixProvider.class);

    private final String appName;

    private final String contextPath;

    public UriPrefixProvider(@Value("${spring.application.name:}") String applicationName,
                              @Value("${server.servlet.context-path:}") String contextPath) {
        if (StrUtil.isBlank(applicationName)) {
            throw new IllegalArgumentException("请配置 spring.application.name");
        }
        log.info("applicationName:{}", applicationName);
        this.appName = "/" + applicationName;

        if (StrUtil.isNotBlank(contextPath)) {
            this.contextPath = "/" + contextPath;
        } else {
            this.contextPath = "";
        }
    }

    /**
     * isass v3.0 的微服务前缀，已改到具体接口的 url 定义上，故直接返回空字符串
     *
     * @return uri prefix
     */
    public String getUriPrefix() {
        return "";
    }

}
