// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.security;

import vip.isass.framework.security.AuthenticatedPrincipal;
import vip.isass.framework.security.data.TenantReadPolicy;

/**
 * 业务服务以代码补充关联型租户边界和受信任的平台管理上下文，不接受客户端或管理员规则配置。
 */
public interface TenantReadPolicyProvider {
    TenantReadPolicy policy(AuthenticatedPrincipal principal);

    default boolean allowsPlatformRead(AuthenticatedPrincipal principal) {
        return false;
    }
}
