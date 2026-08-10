// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.netty.packet.impl;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class Content {

    private String cmd;

    private Object body;

}
