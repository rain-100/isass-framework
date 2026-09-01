// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.authentication.internal;

/** 内部微服务 HMAC 请求头。 */
public final class InternalHmacHeaders {

    public static final String SERVICE = "X-ISASS-Internal-Service";
    public static final String KEY_ID = "X-ISASS-Internal-Key-Id";
    public static final String TIMESTAMP = "X-ISASS-Internal-Timestamp";
    public static final String REQUEST_ID = "X-ISASS-Internal-Request-Id";
    public static final String CONTENT_SHA256 = "X-ISASS-Internal-Content-SHA256";
    public static final String SIGNATURE = "X-ISASS-Internal-Signature";
    public static final String UNSIGNED_PAYLOAD = "UNSIGNED-PAYLOAD";

    private InternalHmacHeaders() {
    }
}
