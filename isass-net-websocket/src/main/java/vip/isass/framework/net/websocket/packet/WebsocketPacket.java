// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.websocket.packet;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * @author Rain
 */
@Getter
@Setter
@ToString
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class WebsocketPacket {

    private String cmd;

    private Object payload;

}
