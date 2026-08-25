// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.authorization;

import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.web.filter.OncePerRequestFilter;

import java.util.Collection;

/**
 * 由业务服务贡献的根信任入口安全分支，例如 BSP Bootstrap HMAC。
 */
public interface RootTrustSecurityConfigurer {

    Collection<String> protectedUrls();

    OncePerRequestFilter authenticationFilter();

    AuthorizationManager<RequestAuthorizationContext> authorizationManager();
}
