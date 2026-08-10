// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.socketio.handler;

import cn.hutool.core.exceptions.ExceptionUtil;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.listener.ExceptionListener;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import vip.isass.framework.net.core.handler.manager.EventManager;
import vip.isass.framework.net.core.message.MessageCmd;
import vip.isass.framework.net.core.session.ISessionService;
import vip.isass.framework.net.core.session.Session;

import java.util.List;

/**
 * socketIo 异常事件监听器
 *
 * @author rain
 */
@Slf4j
public class OnSocketIoErrorListener implements ExceptionListener {

    private final EventManager eventManager;

    private final ISessionService sessionService;

    public OnSocketIoErrorListener(EventManager eventManager, ISessionService sessionService) {
        this.eventManager = eventManager;
        this.sessionService = sessionService;
    }

    @Override
    public void onEventException(Exception e, List<Object> args, SocketIOClient client) {
        log.error(e.getMessage(), e);
        Throwable unwrap = ExceptionUtil.unwrap(e);
        log.warn("socketio event exception: {}", unwrap.getMessage());
        client.sendEvent(MessageCmd.ERROR, "发生异常：" + unwrap.getMessage());
    }

    @Override
    public void onDisconnectException(Exception e, SocketIOClient client) {
        Session<?> session = sessionService.getSessionById(client.getSessionId().toString());
        eventManager.onError(session, e);
    }

    @Override
    public void onConnectException(Exception e, SocketIOClient client) {
        Session<?> session = sessionService.getSessionById(client.getSessionId().toString());
        eventManager.onError(session, e);
    }

    @Override
    public void onPingException(Exception e, SocketIOClient client) {
        Session<?> session = sessionService.getSessionById(client.getSessionId().toString());
        eventManager.onError(session, e);
    }

    @Override
    public void onAuthException(Throwable t, SocketIOClient client) {
        Session<?> session = sessionService.getSessionById(client.getSessionId().toString());
        eventManager.onError(session, t);
    }

    @Override
    public void onPongException(Exception e, SocketIOClient client) {
        Session<?> session = sessionService.getSessionById(client.getSessionId().toString());
        eventManager.onError(session, e);
    }

    @Override
    public boolean exceptionCaught(ChannelHandlerContext ctx, Throwable e) {
        return true;
    }
}
