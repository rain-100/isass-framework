// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.core.handler;

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
public class P2pMessage {

    private String fromUserId;

    private String targetUserId;

    private String bizType;

    private Object message;

}
