// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.security.apikey;

import vip.isass.framework.common.security.AuthenticatedPrincipal;

import java.util.Collection;

/** 由基础权限服务实现的 API Key 校验 SPI。 */
public interface ApiKeyAuthenticationService {

    ApiKeyAuthenticationResult authenticate(String apiKey);

    record ApiKeyAuthenticationResult(AuthenticatedPrincipal principal, Collection<String> roleCodes) {
    }
}
