// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.security.apikey;

import vip.isass.framework.common.security.DefaultAuthenticatedPrincipal;

import java.util.Collection;

/** 由基础权限服务实现的 API Key 校验 SPI。 */
public interface ApiKeyAuthenticationService {

    ApiKeyAuthenticationResult authenticate(ApiKeyAuthenticationRequest request);

    /** API Key 权威认证请求；完整凭证只能通过请求体传输。 */
    record ApiKeyAuthenticationRequest(String apiKey) {
    }

    /**
     * API Key 认证结果。
     *
     * <p>主体使用具体类型，保证该结果既可用于本地调用，也可通过 HTTP 合同可靠序列化和反序列化。</p>
     */
    record ApiKeyAuthenticationResult(DefaultAuthenticatedPrincipal principal, Collection<String> roleCodes) {
    }
}
