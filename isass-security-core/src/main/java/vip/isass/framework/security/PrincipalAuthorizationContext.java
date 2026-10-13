// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.security;

import java.util.Collection;
import java.util.List;

/**
 * 已认证主体在当前租户和应用中的授权；permissionCodes 为 null 表示仅装载了角色。
 */
public record PrincipalAuthorizationContext(
        Collection<String> roleCodes,
        Collection<String> permissionCodes,
        Long authorizationVersion,
        Long expireAt) {

    public PrincipalAuthorizationContext {
        roleCodes = roleCodes == null ? List.of() : List.copyOf(roleCodes);
        permissionCodes = permissionCodes == null ? null : List.copyOf(permissionCodes);
    }

    public boolean permissionsLoaded() {
        return permissionCodes != null;
    }

    public static PrincipalAuthorizationContext roles(Collection<String> roleCodes, Long version, Long expireAt) {
        return new PrincipalAuthorizationContext(roleCodes, null, version, expireAt);
    }

    public static PrincipalAuthorizationContext empty() {
        return new PrincipalAuthorizationContext(List.of(), List.of(), 0L, null);
    }
}
