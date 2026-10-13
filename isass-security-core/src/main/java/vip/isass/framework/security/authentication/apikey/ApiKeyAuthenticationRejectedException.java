// SPDX-License-Identifier: LGPL-3.0-only
package vip.isass.framework.security.authentication.apikey;

/**
 * An invalid or expired API Key; transport/storage failures must not use this exception.
 */
public class ApiKeyAuthenticationRejectedException extends RuntimeException {
    public ApiKeyAuthenticationRejectedException(Throwable cause) {
        super("API Key 无效或已失效", cause);
    }
}
