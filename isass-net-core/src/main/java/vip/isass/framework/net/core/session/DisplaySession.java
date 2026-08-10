// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.core.session;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * 会话实体
 * 只是用于 json 可以正常反序列化，展示信息
 */
@Getter
@Setter
@ToString
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DisplaySession {

    private String sessionId;

    private String remoteIp;

    private String remotePort;

    private Long createTime;

    private String type;

}
