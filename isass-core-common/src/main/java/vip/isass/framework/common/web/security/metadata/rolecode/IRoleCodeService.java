// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.web.security.metadata.rolecode;

import vip.isass.framework.common.support.api.ApiService;

import java.util.Collection;

/**
 * 角色编码服务
 *
 * @author Rain
 */
public interface IRoleCodeService extends ApiService {

    Collection<String> findRoleCodesByUri(UriRoleCodesReq roleCodesReq);

    void setRoleCodesByUserIdCache(String userId, Collection<String> roleCodes);

    void setRoleCodesByUriCache(String uri, Collection<String> roleCodes);

    /**
     * 获取指定用户拥有的角色
     *
     * @param userId user id
     * @return role codes
     */
    Collection<String> findRoleCodesByUserId(String userId);

}
