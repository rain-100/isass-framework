// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.authentication.jwt;

/**
 * @author Rain
 */
public class JwtUser {

    /**
     * 登录的终端
     */
    private String loginFrom;

    /**
     * 当被验证的 token 的 version 小于本值时，被验证的 token 强制失效
     */
    private Integer forceInvalidBeforeVersion;

    /**
     * 任意端登录时，记录 version 加1
     * 根据 version 判断被验证的 token 是否有效
     */
    private Integer version;

}
