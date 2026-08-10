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
public class PriorityTerminal {

    /**
     * 应用 id
     */
    private Long appId;

    /**
     * 优先终端
     * 如果匹配到互斥终端，则根据此配置判断新端不能登录或旧端需要下线
     * <br>
     * 同一互斥组只只能有一个是优先终端。如果都是 false，则互踢
     */
    private Boolean prefer;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PriorityTerminal)) return false;
        PriorityTerminal that = (PriorityTerminal) o;
        return Objects.equals(appId, that.appId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(appId);
    }
}
