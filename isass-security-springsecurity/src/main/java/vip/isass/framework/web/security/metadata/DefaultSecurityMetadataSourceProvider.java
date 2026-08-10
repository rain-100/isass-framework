// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.metadata;

import org.springframework.stereotype.Component;
import vip.isass.framework.common.web.security.metadata.rolecode.IRoleCodeService;
import vip.isass.framework.common.web.security.metadata.rolecode.UriRoleCodesReq;

import jakarta.annotation.Resource;
import java.util.Collection;

/**
 * 获取权限元数据
 *
 * @author Rain
 */
@Component
public class DefaultSecurityMetadataSourceProvider implements SecurityMetadataSourceProvider {

    @Resource
    private IRoleCodeService roleCodeService;

    /**
     * 获取指定用户拥有的角色
     */
    @Override
    public Collection<String> findRoleCodesByUserId(String userId) {
        return roleCodeService.findRoleCodesByUserId(userId);
    }

    /**
     * 获取访问指定 uri 需要的角色
     */
    @Override
    public Collection<String> findRoleCodesByUri(String uri) {
        return roleCodeService.findRoleCodesByUri(new UriRoleCodesReq().setUri(uri));
    }

}
