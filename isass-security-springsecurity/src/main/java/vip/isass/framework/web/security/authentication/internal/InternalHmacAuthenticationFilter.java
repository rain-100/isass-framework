// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.authentication.internal;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import vip.isass.framework.common.security.InternalServicePrincipal;
import vip.isass.framework.web.security.authentication.PrincipalAuthenticationToken;
import vip.isass.framework.web.security.config.SecurityProperties;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;

/** 校验内部微服务 HMAC，并把调用服务主体合并到当前请求认证上下文。 */
@Slf4j
public final class InternalHmacAuthenticationFilter extends OncePerRequestFilter {

    private final SecurityProperties.Internal properties;

    public InternalHmacAuthenticationFilter(SecurityProperties securityProperties) {
        this.properties = securityProperties.getInternal();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!hasAnyInternalHeader(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        HttpServletRequest effectiveRequest;
        try {
            String serviceName = required(request, InternalHmacHeaders.SERVICE);
            String keyId = required(request, InternalHmacHeaders.KEY_ID);
            long timestamp = Long.parseLong(required(request, InternalHmacHeaders.TIMESTAMP));
            String requestId = required(request, InternalHmacHeaders.REQUEST_ID);
            String declaredHash = required(request, InternalHmacHeaders.CONTENT_SHA256);
            String declaredSignature = required(request, InternalHmacHeaders.SIGNATURE);
            verifyTimestamp(timestamp);
            String secret = properties.verificationKeys().get(keyId);
            if (secret == null || secret.isBlank()) {
                throw new SecurityException("内部 HMAC keyId 无效");
            }

            effectiveRequest = request;
            if (!InternalHmacHeaders.UNSIGNED_PAYLOAD.equals(declaredHash)) {
                byte[] body = request.getInputStream().readAllBytes();
                String actualHash = InternalHmacUtil.contentSha256(body);
                if (!InternalHmacUtil.constantTimeEquals(actualHash, declaredHash)) {
                    throw new SecurityException("内部请求体摘要错误");
                }
                effectiveRequest = new CachedBodyRequest(request, body);
            }
            String expected = InternalHmacUtil.signature(serviceName, keyId, timestamp, requestId,
                    request.getMethod(), request.getRequestURI(), request.getQueryString(), declaredHash, secret);
            if (!InternalHmacUtil.constantTimeEquals(expected, declaredSignature)) {
                throw new SecurityException("内部请求签名错误");
            }
            saveInternalPrincipal(new InternalServicePrincipal(serviceName, keyId, requestId));
        } catch (RuntimeException exception) {
            log.warn("内部 HMAC 认证失败: {}", exception.getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "内部服务认证失败");
            return;
        }
        filterChain.doFilter(effectiveRequest, response);
    }

    private void saveInternalPrincipal(InternalServicePrincipal internalPrincipal) {
        Authentication current = SecurityContextHolder.getContext().getAuthentication();
        if (current instanceof PrincipalAuthenticationToken token) {
            if (token.hasInternalServicePrincipal()) {
                throw new SecurityException("同一请求不能重复携带内部服务身份");
            }
            SecurityContextHolder.getContext().setAuthentication(new PrincipalAuthenticationToken(
                    token.getPrincipal(), internalPrincipal, token.getAuthorities(), token.getAuthorizationContext()));
            return;
        }
        SecurityContextHolder.getContext().setAuthentication(new PrincipalAuthenticationToken(
                null, internalPrincipal, Collections.emptyList(), null));
    }

    private boolean hasAnyInternalHeader(HttpServletRequest request) {
        return request.getHeader(InternalHmacHeaders.SERVICE) != null
                || request.getHeader(InternalHmacHeaders.KEY_ID) != null
                || request.getHeader(InternalHmacHeaders.TIMESTAMP) != null
                || request.getHeader(InternalHmacHeaders.REQUEST_ID) != null
                || request.getHeader(InternalHmacHeaders.CONTENT_SHA256) != null
                || request.getHeader(InternalHmacHeaders.SIGNATURE) != null;
    }

    private String required(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        if (value == null || value.isBlank()) throw new SecurityException(name + " 必填");
        return value;
    }

    private void verifyTimestamp(long timestamp) {
        Duration allowed = properties.getAllowedClockSkew() == null
                ? Duration.ofMinutes(5) : properties.getAllowedClockSkew();
        long skew = Math.max(0L, allowed.toMillis());
        long now = System.currentTimeMillis();
        if (timestamp < now - skew || timestamp > now + skew) {
            throw new SecurityException("内部请求时间戳已过期");
        }
    }

    private static final class CachedBodyRequest extends HttpServletRequestWrapper {

        private final byte[] body;

        private CachedBodyRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream input = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override public boolean isFinished() { return input.available() == 0; }
                @Override public boolean isReady() { return true; }
                @Override public void setReadListener(ReadListener readListener) { }
                @Override public int read() { return input.read(); }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(),
                    getCharacterEncoding() == null ? StandardCharsets.UTF_8
                            : java.nio.charset.Charset.forName(getCharacterEncoding())));
        }
    }
}
