// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.socketio.handler;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.annotation.OnConnect;
import lombok.extern.slf4j.Slf4j;
import vip.isass.framework.net.core.handler.manager.EventManager;
import vip.isass.framework.net.socketio.SocketIoSession;

/**
 * socketIo  新建连接事件监听器
 *
 * @author rain
 */
@Slf4j
public class OnSocketIoConnectListener {

    private final EventManager eventManager;

    public OnSocketIoConnectListener(EventManager eventManager) {
        this.eventManager = eventManager;
    }

    @OnConnect
    public void onConnect(SocketIOClient client) {
        SocketIoSession socketIoSession = new SocketIoSession(client);
        eventManager.onConnect(socketIoSession);
    }

}
