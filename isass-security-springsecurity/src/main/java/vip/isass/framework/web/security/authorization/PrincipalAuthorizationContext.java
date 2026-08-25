// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.authorization;

import java.util.Collection;
import java.util.List;

/**
 * 一个已认证主体在当前租户和应用中的完整功能授权上下文。
 */
public record PrincipalAuthorizationContext(
        Collection<String> roleCodes,
        Collection<String> permissionCodes,
        Long authorizationVersion,
        Long expireAt) {

    public PrincipalAuthorizationContext {
        roleCodes = roleCodes == null ? List.of() : List.copyOf(roleCodes);
        permissionCodes = permissionCodes == null ? List.of() : List.copyOf(permissionCodes);
    }

    public static PrincipalAuthorizationContext empty() {
        return new PrincipalAuthorizationContext(List.of(), List.of(), 0L, null);
    }
}
