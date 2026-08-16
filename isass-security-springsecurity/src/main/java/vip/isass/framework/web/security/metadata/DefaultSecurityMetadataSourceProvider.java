// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.metadata;

import org.springframework.stereotype.Component;
import vip.isass.framework.web.security.authorization.IAuthorizationService;
import vip.isass.framework.web.security.authorization.UriRoleCodesRequest;

import java.util.Collection;

/**
 * 获取权限元数据
 *
 * @author Rain
 */
@Component
public class DefaultSecurityMetadataSourceProvider implements SecurityMetadataSourceProvider {

    private final IAuthorizationService authorizationService;

    public DefaultSecurityMetadataSourceProvider(IAuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    /**
     * 获取指定用户拥有的角色
     */
    @Override
    public Collection<String> findRoleCodesByUserId(String userId) {
        return authorizationService.findRoleCodesByUserId(userId);
    }

    /**
     * 获取访问指定 uri 需要的角色
     */
    @Override
    public Collection<String> findRoleCodesByUri(String uri) {
        return authorizationService.findRoleCodesByUri(new UriRoleCodesRequest(uri));
    }

}
