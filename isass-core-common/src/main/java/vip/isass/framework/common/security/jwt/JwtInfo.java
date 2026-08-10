// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.security.jwt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import vip.isass.framework.common.login.TerminalType;
import vip.isass.framework.common.security.PrincipalType;

/**
 * JWT 信息
 *
 * @author Rain
 */
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class JwtInfo {

    public static final String USER_ID = "uid";

    public static final String NICK_NAME = "name";

    public static final String TENANT_ID = "tid";

    public static final String APP_ID = "aid";

    public static final String APP_GROUP_ID = "agid";

    public static final String TERMINAL_TYPE = "tt";

    public static final String LOGIN_LOG_ID = "lid";

    public static final String EXPIRE_AT = "eat";

    /**
     * 用户 id
     */
    private Long uid;

    /**
     * 用户昵称
     */
    private String name;

    /**
     * 租户 id
     */
    private Long tid;

    /**
     * 应用 id
     */
    private Long aid;

    /**
     * 应用组 id
     */
    private Long agid;

    /**
     * 终端类型
     */
    private TerminalType tt;

    /**
     * 登录日志 id
     */
    private Long lid;

    /** JWT 认证到期时间，毫秒时间戳。 */
    private Long eat;

}
