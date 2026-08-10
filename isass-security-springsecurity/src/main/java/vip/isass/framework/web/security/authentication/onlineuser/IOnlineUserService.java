// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.authentication.onlineuser;

import vip.isass.framework.common.security.AuthenticatedPrincipal;

import java.util.List;

/**
 * 用户在线管理服务
 */
public interface IOnlineUserService {

    void offline(AuthenticatedPrincipal principal);

    List<AuthenticatedPrincipal> getOnlineUsers(Long tenantId, String userId);

}
