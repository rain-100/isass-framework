// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.socketio;

import cn.hutool.core.lang.Assert;
import com.corundumstudio.socketio.SocketIOClient;
import lombok.extern.slf4j.Slf4j;
import vip.isass.framework.common.support.SystemClock;
import vip.isass.framework.net.core.session.ClientSession;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * socketIo 客户端会话
 *
 * @author Rain
 */
@Slf4j
public class SocketIoSession implements ClientSession<SocketIoServer> {

    private SocketIOClient socketIoClient;

    /**
     * 创建session的时间
     */
    private Long createTime;

    private SocketIoSession() {
    }

    public SocketIoSession(SocketIOClient socketIoClient) {
        Assert.notNull(socketIoClient, "socketIoClient 必填");
        this.socketIoClient = socketIoClient;
        this.createTime = SystemClock.now();
    }

    @Override
    public boolean isActive() {
        return socketIoClient.isChannelOpen();
    }

    @Override
    public void close() {
        socketIoClient.disconnect();
    }

    @Override
    public String getRemoteIp() {
        SocketAddress remoteAddress = socketIoClient.getRemoteAddress();
        if (remoteAddress instanceof InetSocketAddress) {
            return ((InetSocketAddress) remoteAddress)
                    .getAddress()
                    .getHostAddress();
        }
        return remoteAddress.toString();
    }

    @Override
    public String getRemotePort() {
        SocketAddress remoteAddress = socketIoClient.getRemoteAddress();
        if (remoteAddress instanceof InetSocketAddress) {
            return ((InetSocketAddress) remoteAddress)
                    .getPort() + "";

        }
        return remoteAddress.toString();
    }

    @Override
    public String getSessionId() {
        return socketIoClient.getSessionId().toString();
    }

    @Override
    public Long getCreateTime() {
        return createTime;
    }

    @Override
    public void sendMessage(String cmd, Object payload) {
        log.trace("向会话[{}]发送: {} {}", getSessionId(), cmd, payload);
        socketIoClient.sendEvent(cmd, payload);
    }

    public SocketIOClient getSocketIoClient() {
        return socketIoClient;
    }

    @Override
    public String toString() {
        return print();
    }

}
