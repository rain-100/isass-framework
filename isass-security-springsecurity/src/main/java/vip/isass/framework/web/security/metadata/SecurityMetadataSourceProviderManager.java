// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.metadata;

import org.springframework.stereotype.Service;
import vip.isass.framework.common.support.FunctionUtil;
import vip.isass.framework.web.security.config.UrlAccessSecurityStrategy;
import vip.isass.framework.web.security.config.SecurityProperties;

import jakarta.annotation.Resource;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Service
public class SecurityMetadataSourceProviderManager {

    @Resource
    private List<SecurityMetadataSourceProvider> providers;

    @Resource
    private SecurityProperties securityProperties;

    public Collection<String> findRoleCodesByUserId(String userId) {
        return securityProperties.getUrlAccessSecurityStrategy() == UrlAccessSecurityStrategy.ROLE
            ? FunctionUtil.getFirstNotNullValueFromCollection(providers, service -> service.findRoleCodesByUserId(userId))
            : Collections.emptyList();
    }

    public Collection<String> findRoleCodesByUri(String uri) {
        return securityProperties.getUrlAccessSecurityStrategy() == UrlAccessSecurityStrategy.ROLE
            ? FunctionUtil.getFirstNotNullValueFromCollection(providers, service -> service.findRoleCodesByUri(uri))
            : Collections.emptyList();
    }

}
