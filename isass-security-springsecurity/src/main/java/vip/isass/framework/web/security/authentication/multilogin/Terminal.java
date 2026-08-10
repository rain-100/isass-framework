// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.authentication.multilogin;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.Objects;

/**
 * 终端运行时参数
 */
@Getter
@Setter
@ToString
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Terminal {

    /**
     * 设备类型
     */
    private String deviceType;

    /**
     * 操作系统
     */
    private String os;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Terminal terminal = (Terminal) o;
        return Objects.equals(deviceType, terminal.deviceType) && Objects.equals(os, terminal.os);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deviceType, os);
    }
}
