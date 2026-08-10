// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.login;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * 终端运行时参数
 */
@Getter
@Setter
@ToString
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TerminalType {

    /**
     * 设备类型
     */
    private String deviceType;

    /**
     * 操作系统
     */
    private String os;

    /**
     * 设备品牌
     */
    private String brand;

    /**
     * 浏览器类型
     * 如果是混合架构开发，则应视为 app，不用填写浏览器类型
     */
    private String browserType;

    /**
     * 网络类型
     */
    private String netType;

}
