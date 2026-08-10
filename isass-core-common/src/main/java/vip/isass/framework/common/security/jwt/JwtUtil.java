// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.security.jwt;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.SystemClock;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import vip.isass.framework.common.exception.UnifiedException;
import vip.isass.framework.common.exception.code.StatusMessageEnum;
import vip.isass.framework.common.security.AuthenticatedPrincipal;
import vip.isass.framework.common.support.LocalDateTimeUtil;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;

/**
 * JWT 工具类
 *
 * @author Rain
 */
public class JwtUtil {

    public static long TOKEN_EFFECTIVE_SECONDS = ChronoUnit.WEEKS.getDuration().getSeconds();

    public static long TOKEN_EFFECTIVE_MILLS = TOKEN_EFFECTIVE_SECONDS * 1000;

    public static final String DEFAULT_SECRET = "siiwwsQgwxGgFhFdZ8cdYtjixrxCHJRzFBRDWwt4EdFkyz2SJf4uN8IhSAFtD19C";

    public static String generateToken(AuthenticatedPrincipal principal, String secret) {
        long expireAt = principal.getAuthenticationExpireAt() == null
                ? SystemClock.now() + TOKEN_EFFECTIVE_MILLS
                : principal.getAuthenticationExpireAt();
        // 生产 token
        Map<String, Object> map = MapUtil.<String, Object>builder()
                .put(JwtInfo.TENANT_ID, principal.getTenantId())
                .put(JwtInfo.APP_ID, principal.getAppId())
                .put(JwtInfo.USER_ID, principal.getPrincipalId())
                .put(JwtInfo.NICK_NAME, principal.getPrincipalName())
                .put(JwtInfo.TERMINAL_TYPE, principal.getTerminalType())
                .put(JwtInfo.LOGIN_LOG_ID, principal.getLoginLogId())
                .put(JwtInfo.EXPIRE_AT, expireAt)
                .build();

        secret = StrUtil.blankToDefault(secret, DEFAULT_SECRET);
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .claims(map)
                .expiration(new Date(expireAt))
                .signWith(key)
                .compact();
    }

    public static JwtInfo parse(String token, String secret) {
        secret = StrUtil.blankToDefault(secret, DEFAULT_SECRET);
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        Claims claims;
        try {
            claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new UnifiedException(StatusMessageEnum.TOKEN_EXPIRED);
        } catch (Exception e) {
            throw new UnifiedException(StatusMessageEnum.TOKEN_ILLEGAL);
        }

        // 判断 token 是否过期
        Date expiration = claims.getExpiration();
        if (expiration.before(LocalDateTimeUtil.nowDate())) {
            throw new UnifiedException(StatusMessageEnum.TOKEN_EXPIRED);
        }

        return BeanUtil.toBean(claims, JwtInfo.class);
    }

}
