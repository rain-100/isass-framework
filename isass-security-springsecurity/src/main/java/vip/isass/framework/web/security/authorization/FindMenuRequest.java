// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.authorization;

/** Scope used to resolve the navigation menu visible to a user. */
public record FindMenuRequest(Long userId, Long tenantId, Long appId) {
}
