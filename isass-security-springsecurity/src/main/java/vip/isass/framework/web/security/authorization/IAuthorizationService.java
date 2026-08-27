// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.authorization;

import vip.isass.framework.common.security.PrincipalType;
import vip.isass.framework.entrypoint.IEntrypoint;
import vip.isass.framework.entrypoint.annotation.BodyParam;
import vip.isass.framework.entrypoint.annotation.EntrypointInfo;
import vip.isass.framework.entrypoint.annotation.EntrypointOperation;
import vip.isass.framework.entrypoint.annotation.QueryParam;
import vip.isass.framework.entrypoint.metadata.HttpMethod;

import java.util.Collection;
import java.util.List;

/**
 * Framework-level authorization entrypoint implemented by bsp-service.
 * Consumers inject this interface directly; the entrypoint registry chooses a local bean or a remote proxy.
 */
@EntrypointInfo(serviceName = "bsp-service", contextName = "auth", resourceName = "authorization")
public interface IAuthorizationService extends IEntrypoint {

    @EntrypointOperation(operationName = "apiKeyContext", displayName = "API Key 授权上下文",
            httpMethod = HttpMethod.POST, allowAnonymous = true)
    ApiKeyAuthenticationResult apiKeyContext(@BodyParam ApiKeyAuthenticationRequest request);

    @EntrypointOperation(operationName = "jwtContext", displayName = "JWT 授权上下文",
            httpMethod = HttpMethod.GET, allowAnonymous = true)
    PrincipalAuthorizationContext jwtContext();

    /**
     * BSP 本地实现使用的统一计算入口，不生成 HTTP 或 OpenAPI。
     */
    PrincipalAuthorizationContext findAuthorizationContext(PrincipalType principalType,
                                                           Long principalId,
                                                           Long tenantId,
                                                           Long appId);

    @EntrypointOperation(operationName = "findRoleCodes", displayName = "查询主体角色",
            httpMethod = HttpMethod.GET)
    Collection<String> findRoleCodes(@QueryParam("principalType") PrincipalType principalType,
                                     @QueryParam("principalId") Long principalId);

    @EntrypointOperation(operationName = "findRoleCodesByUserId", displayName = "查询用户角色",
            httpMethod = HttpMethod.GET)
    Collection<String> findRoleCodesByUserId(@QueryParam("userId") String userId);

    @EntrypointOperation(operationName = "clearAuthenticationCaches", displayName = "清空认证缓存",
            httpMethod = HttpMethod.POST)
    void clearAuthenticationCaches();

    @EntrypointOperation(operationName = "findAccessibleResources", displayName = "查询可访问资源",
            httpMethod = HttpMethod.POST)
    List<AuthorizationResource> findAccessibleResources(@BodyParam FindAccessibleResourceRequest request);

    @EntrypointOperation(operationName = "findMenuTrees", displayName = "查询可见菜单树",
            httpMethod = HttpMethod.GET)
    List<MenuTree> findMenuTrees(@QueryParam FindMenuRequest request);
}
