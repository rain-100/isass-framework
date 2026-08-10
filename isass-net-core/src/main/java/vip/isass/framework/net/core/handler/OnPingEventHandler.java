// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.core.handler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import vip.isass.framework.net.core.message.Message;
import vip.isass.framework.net.core.message.MessageCmd;
import vip.isass.framework.net.core.server.Server;

/**
 * ping 事件处理器
 *
 * @author rain
 */
@Configuration
@ConditionalOnBean(Server.class)
public class OnPingEventHandler implements OnMessageEventHandler<String> {

    @Override
    public String getCmd() {
        return MessageCmd.PING;
    }

    @Override
    public Object onMessage(Message message, String ping) {
        message.getSenderSession().sendMessage(MessageCmd.PONG, "");
        return null;
    }

}
