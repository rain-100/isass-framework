// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.security;

/**
 * 提供当前请求已认证主体。
 */
public interface CurrentPrincipalService {

    AuthenticatedPrincipal getPrincipal();
}
