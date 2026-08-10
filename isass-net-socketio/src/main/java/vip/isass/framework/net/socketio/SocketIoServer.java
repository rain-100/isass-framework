// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.socketio;

import com.corundumstudio.socketio.SocketIOServer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import vip.isass.framework.net.core.server.NetProtocol;
import vip.isass.framework.net.core.server.Server;

/**
 * socketIo 服务端
 *
 * @author Rain
 */
@Slf4j
public class SocketIoServer implements Server {

    @Getter
    private final SocketIOServer socketIoServer;

    public SocketIoServer(SocketIOServer socketIoServer) {
        this.socketIoServer = socketIoServer;
    }

    @Override
    public String getListeningAddress() {
        return socketIoServer.getConfiguration().getHostname() + ":" + socketIoServer.getConfiguration().getPort();
    }

    @Override
    public void start() {
        socketIoServer.start();
    }

    @Override
    public void stop() {
        if (socketIoServer != null) {
            socketIoServer.stop();
        }
    }

    @Override
    public NetProtocol netProtocol() {
        return NetProtocol.socketio;
    }

}
