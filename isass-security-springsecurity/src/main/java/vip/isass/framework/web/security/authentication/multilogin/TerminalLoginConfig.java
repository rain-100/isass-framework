// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.authentication.multilogin;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.Set;

/**
 * <p>
 * 多端登陆配置
 * </p>
 *
 * @author isass
 */
@Getter
@Setter
@ToString
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TerminalLoginConfig {

    /**
     * 主键
     */
    private Long id;

    /**
     * 策略名称
     */
    private String name;

    /**
     * 在线上限数[超过上限则不能登录。最大127]
     */
    private Integer onlineLimit;

    /**
     * 互斥终端[列表中只能同时有一种端在线]
     */
    private Set<TerminalGroup> mutexTerminals;

    /**
     * 同端多登[同种终端同时在线上限]
     */
    private Set<SameTerminalProperty> sameTerminals;

    /**
     * 租户ID
     */
    private Long tenantId;

}