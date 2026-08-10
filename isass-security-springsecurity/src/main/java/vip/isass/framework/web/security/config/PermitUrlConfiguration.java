// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.config;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import vip.isass.framework.web.security.PermitUrlProvider;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Rain
 */
@Getter
@Setter
@Accessors(chain = true)
@ToString
@Configuration
@ConfigurationProperties("isass.security.permit-url")
public class PermitUrlConfiguration {

    @Autowired(required = false)
    private List<PermitUrlProvider> permitUrlProviders;

    /**
     * 配置文件配置的url
     */
    private List<String> permitUrls;

    public Collection<String> getPermitUrls() {
        HashSet<String> permitUrls = CollUtil.newHashSet(
            "/error",
                "/favicon.ico"
        );

        if (permitUrlProviders != null) {
            permitUrls.addAll(permitUrlProviders.stream()
                .map(PermitUrlProvider::getUrls)
                .filter(CollUtil::isNotEmpty)
                .flatMap(Collection::stream)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toList()));
        }
        return CollUtil.isEmpty(this.permitUrls)
            ? permitUrls
            : CollUtil.addAll(permitUrls, this.permitUrls);
    }

}
