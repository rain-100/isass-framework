// SPDX-License-Identifier: LGPL-3.0-only
package vip.isass.framework.security.authentication.apikey;

/**
 * Validates an API Key and resolves its application identity and authorization.
 * Credential rejection is distinct from infrastructure failure.
 */
@FunctionalInterface
public interface ApiKeyAuthenticator {
    ApiKeyAuthenticationResult authenticate(ApiKeyAuthenticationRequest request);
}
