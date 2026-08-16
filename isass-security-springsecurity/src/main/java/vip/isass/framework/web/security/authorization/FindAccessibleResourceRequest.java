// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.authorization;

/** Scope used to resolve resources visible to a user. */
public record FindAccessibleResourceRequest(Long userId, Long tenantId, Long appId, Integer type) {
}
