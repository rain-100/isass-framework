// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.socketio;

import cn.hutool.core.collection.CollUtil;
import vip.isass.framework.net.core.handler.IMessageEventRegister;
import vip.isass.framework.net.core.handler.manager.EventManager;
import vip.isass.framework.net.core.message.Message;
import vip.isass.framework.net.core.session.ISessionService;
import vip.isass.framework.net.core.session.Session;

import java.util.Collection;

/**
 * socketIo 消息事件监听器
 *
 * @author rain
 */
public class SocketIoEventHandlerRegister implements IMessageEventRegister {

    private final SocketIoServer socketIoServer;

    private final ISessionService sessionService;

    private final EventManager eventManager;

    public SocketIoEventHandlerRegister(SocketIoServer socketIoServer, ISessionService sessionService, EventManager eventManager) {
        this.socketIoServer = socketIoServer;
        this.sessionService = sessionService;
        this.eventManager = eventManager;
    }

    @Override
    public void listening(Collection<String> commands) {
        if (socketIoServer == null) {
            return;
        }

        for (String cmd : commands) {
            socketIoServer.getSocketIoServer()
                    .addEventListener(
                            cmd,
                            Object.class,
                            (client, data, ackSender) -> {
                                Session<?> session = sessionService.getSessionById(client.getSessionId().toString());
                                eventManager.onMessage(
                                        Message.builder()
                                                .senderSessionId(session.getSessionId())
                                                .senderSession(session)
                                                .cmd(cmd)
                                                .payload(data)
                                                .build());
                            });
        }
    }

    @Override
    public void removeListening(Collection<String> commands) {
        if (CollUtil.isEmpty(commands)) {
            return;
        }
        commands.forEach(c -> socketIoServer.getSocketIoServer().removeAllListeners(c));
    }

}
