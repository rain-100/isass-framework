// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.core.handler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import vip.isass.framework.net.core.message.Message;
import vip.isass.framework.net.core.message.MessageCmd;
import vip.isass.framework.net.core.server.Server;
import vip.isass.framework.net.core.session.ISessionService;

import jakarta.annotation.Resource;

/**
 * 客户端发送的广播事件处理器
 *
 * @author rain
 */
@Order(-1)
@Configuration
@ConditionalOnBean(Server.class)
public class OnClientSendBroadcastEventHandler implements OnMessageEventHandler<Object> {

    @Resource
    private ISessionService sessionService;

    @Override
    public String getCmd() {
        return MessageCmd.CLIENT_SEND_BROADCAST;
    }

    @Override
    public Object onMessage(Message message, Object payload) {
        sessionService.broadcastMessage(MessageCmd.CLIENT_SEND_BROADCAST, payload);
        return null;
    }

}
