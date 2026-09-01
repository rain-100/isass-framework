// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.authentication.internal;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.stream.Collectors;

/** 内部 HTTP 调用的 HMAC-SHA256 规范化与签名工具。 */
public final class InternalHmacUtil {

    private static final String SCOPE = "isass-internal-v1";

    private InternalHmacUtil() {
    }

    public static String contentSha256(byte[] body) {
        try {
            return base64Url(MessageDigest.getInstance("SHA-256")
                    .digest(body == null ? new byte[0] : body));
        } catch (Exception exception) {
            throw new IllegalStateException("内部请求摘要计算失败", exception);
        }
    }

    public static String signature(String serviceName, String keyId, long timestamp,
                                   String requestId, String method, String path,
                                   String rawQuery, String contentSha256, String secret) {
        requireText(serviceName, "内部调用服务名");
        requireText(keyId, "内部 HMAC keyId");
        requireText(requestId, "内部请求 ID");
        requireText(method, "HTTP method");
        requireText(path, "HTTP path");
        requireText(contentSha256, "请求体摘要");
        requireText(secret, "内部 HMAC Secret");
        String canonical = String.join("\n", SCOPE, serviceName, keyId, String.valueOf(timestamp),
                requestId, method.toUpperCase(), path, normalizeQuery(rawQuery), contentSha256);
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return base64Url(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("内部请求 HMAC 计算失败", exception);
        }
    }

    public static boolean constantTimeEquals(String left, String right) {
        if (left == null || right == null) return false;
        return MessageDigest.isEqual(left.getBytes(StandardCharsets.US_ASCII),
                right.getBytes(StandardCharsets.US_ASCII));
    }

    private static String normalizeQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) return "";
        return Arrays.stream(rawQuery.split("&"))
                .sorted()
                .collect(Collectors.joining("&"));
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " 必填");
        }
        return value;
    }

    private static String base64Url(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }
}
