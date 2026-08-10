// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.websocket.session;

import cn.hutool.core.lang.Assert;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.extern.slf4j.Slf4j;
import vip.isass.framework.common.support.JsonUtil;
import vip.isass.framework.common.support.SystemClock;
import vip.isass.framework.net.core.session.ClientSession;
import vip.isass.framework.net.websocket.packet.WebsocketPacket;
import vip.isass.framework.net.websocket.websocket.WebsocketServer;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * tcp 客户端会话
 *
 * @author Rain
 */
@Slf4j
public class WebsocketClientSession implements ClientSession<WebsocketServer> {

    /**
     * 与客户端的链接通道
     */
    private Channel channel;

    /**
     * 创建session的时间
     */
    private Long createTime;

    private WebsocketClientSession() {
    }

    public WebsocketClientSession(Channel channel) {
        Assert.notNull(channel, "channel不能为null");
        this.channel = channel;
        this.createTime = SystemClock.now();
    }

    @Override
    public boolean isActive() {
        return channel.isActive();
    }

    @Override
    public void close() {
        if (channel == null) {
            return;
        }
        channel.close();
    }

    @Override
    public String getRemoteIp() {
        SocketAddress remoteAddress = channel.remoteAddress();
        if (remoteAddress instanceof InetSocketAddress) {
            return ((InetSocketAddress) remoteAddress)
                    .getAddress()
                    .getHostAddress();
        }
        return remoteAddress.toString();
    }

    @Override
    public String getRemotePort() {
        SocketAddress remoteAddress = channel.remoteAddress();
        if (remoteAddress instanceof InetSocketAddress) {
            return ((InetSocketAddress) remoteAddress)
                    .getPort() + "";

        }
        return remoteAddress.toString();
    }

    @Override
    public String getSessionId() {
        return channel.id().toString();
    }

    @Override
    public Long getCreateTime() {
        return createTime;
    }

    /**
     * 原则上系统向客户端发消息，均统一调用此方法
     */
    @Override
    public void sendMessage(String cmd, Object payload) {
        if (!isActive()) {
            log.debug("channel is inactive, send Message fail. session info: {}", this);
            return;
        }

        TextWebSocketFrame textWebSocketFrame = new TextWebSocketFrame(JsonUtil.writeValue(WebsocketPacket.builder()
                .cmd(cmd)
                .payload(payload)
                .build()));
        channel.writeAndFlush(textWebSocketFrame);
    }


    public Channel getChannel() {
        return channel;
    }

    @Override
    public String toString() {
        return print();
    }
}
