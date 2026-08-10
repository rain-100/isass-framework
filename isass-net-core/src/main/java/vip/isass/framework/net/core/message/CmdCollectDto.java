// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.core.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Objects;

/**
 * @author rain
 */
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CmdCollectDto {

    /**
     * 路由命令
     */
    private String cmd;

    /**
     * 最新一次的收集时间
     */
    private Long collectTime;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CmdCollectDto that = (CmdCollectDto) o;
        return Objects.equals(cmd, that.cmd);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cmd);
    }

}
