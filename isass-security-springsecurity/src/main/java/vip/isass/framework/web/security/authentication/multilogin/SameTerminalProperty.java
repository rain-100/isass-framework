// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.authentication.multilogin;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@ToString
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SameTerminalProperty {

    private Long appId;

    private Integer maxOnlineCount;

    /**
     * 如果同端登录已达到上限值，是否新登录的更加优先。(即新登录可以成功，需要踢一个旧端下线)
     */
    private Boolean preferNewLogin;

}
