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
 * 客户端发起p2p消息
 *
 * @author rain
 */
@Order(-1)
@Configuration
@ConditionalOnBean(Server.class)
public class OnClientP2pEventHandler implements OnMessageEventHandler<P2pMessage> {

    @Resource
    private ISessionService sessionService;

    @Override
    public String getCmd() {
        return MessageCmd.CLIENT_P2P;
    }

    @Override
    public Object onMessage(Message message, P2pMessage payload) {
        sessionService.sendMessageByUserId(
                MessageCmd.CLIENT_P2P,
                payload,
                payload.getTargetUserId());
        return null;
    }

}
