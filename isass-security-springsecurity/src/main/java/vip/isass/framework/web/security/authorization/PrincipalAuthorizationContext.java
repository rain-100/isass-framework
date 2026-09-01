// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.authorization;

import java.util.Collection;
import java.util.List;

/**
 * 一个已认证主体在当前租户和应用中的完整功能授权上下文。
 */
public record PrincipalAuthorizationContext(
        Collection<AuthorizationRole> roles,
        Collection<String> roleCodes,
        Collection<String> permissionCodes,
        Long authorizationVersion,
        Long expireAt) {

    public PrincipalAuthorizationContext {
        roles = roles == null ? List.of() : List.copyOf(roles);
        roleCodes = roleCodes == null ? List.of() : List.copyOf(roleCodes);
        permissionCodes = permissionCodes == null ? List.of() : List.copyOf(permissionCodes);
    }

    public PrincipalAuthorizationContext(Collection<String> roleCodes,
                                         Collection<String> permissionCodes,
                                         Long authorizationVersion,
                                         Long expireAt) {
        this(List.of(), roleCodes, permissionCodes, authorizationVersion, expireAt);
    }

    public static PrincipalAuthorizationContext empty() {
        return new PrincipalAuthorizationContext(List.of(), List.of(), List.of(), 0L, null);
    }
}
