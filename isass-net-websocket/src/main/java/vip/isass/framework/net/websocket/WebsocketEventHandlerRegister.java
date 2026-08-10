// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.websocket;

import vip.isass.framework.net.core.handler.IMessageEventRegister;

import java.util.Collection;

/**
 * websocket 消息事件监听器
 * websocket 无需提前监听事件，即可收到消息
 *
 * @author rain
 */
public class WebsocketEventHandlerRegister implements IMessageEventRegister {

    @Override
    public void listening(Collection<String> commands) {
        // do nothing;
    }

    @Override
    public void removeListening(Collection<String> commands) {
        // do nothing;
    }

}
