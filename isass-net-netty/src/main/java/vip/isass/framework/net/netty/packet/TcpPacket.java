// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.netty.packet;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * tcp 数据包
 *
 * @author Rain
 */
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TcpPacket implements IPacket {

    Integer fullLength;

    Integer serializeMode;

    Integer cmdLength;

    String cmd;

    Object payload;

}
