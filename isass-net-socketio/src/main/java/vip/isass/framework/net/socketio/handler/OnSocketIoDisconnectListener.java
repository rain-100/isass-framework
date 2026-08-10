// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.socketio.handler;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.annotation.OnDisconnect;
import lombok.extern.slf4j.Slf4j;
import vip.isass.framework.net.core.session.ISessionService;
import vip.isass.framework.net.core.session.Session;
import vip.isass.framework.net.core.handler.manager.EventManager;

/**
 * socketIo 断开连接事件监听器
 *
 * @author rain
 */
@Slf4j
public class OnSocketIoDisconnectListener {

    private final ISessionService sessionManager;

    private final EventManager eventManager;

    public OnSocketIoDisconnectListener(ISessionService sessionManager, EventManager eventManager) {
        this.sessionManager = sessionManager;
        this.eventManager = eventManager;
    }

    @OnDisconnect
    public void onDisconnect(SocketIOClient client) {
        Session<?> session = sessionManager.getSessionById(client.getSessionId().toString());
        eventManager.onDisconnect(session);
    }

}
