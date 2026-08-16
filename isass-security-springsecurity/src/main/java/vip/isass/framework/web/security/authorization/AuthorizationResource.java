// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.authorization;

/** Cross-service projection of one authorization resource. */
public record AuthorizationResource(
        Long id,
        Long parentId,
        Long tenantId,
        Long appId,
        Integer type,
        String name,
        String uri,
        String httpMethod,
        String routePath,
        String component,
        Integer orderNum,
        Boolean enableFlag
) {
}
