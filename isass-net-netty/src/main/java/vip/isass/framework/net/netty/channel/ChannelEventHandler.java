// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.netty.channel;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import vip.isass.framework.common.support.JsonUtil;
import vip.isass.framework.net.core.message.MessageCmd;
import vip.isass.framework.net.core.session.ISessionService;
import vip.isass.framework.net.core.session.Session;
import vip.isass.framework.net.netty.packet.TcpPacket;
import vip.isass.framework.net.netty.request.Request;
import vip.isass.framework.net.netty.request.RequestManager;
import vip.isass.framework.net.netty.session.TcpClientSession;
import vip.isass.framework.net.netty.tcp.TcpServer;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author Rain
 */
public interface ChannelEventHandler extends ChannelInboundHandler {

    ISessionService getSessionService();

    RequestManager getRequestManager();

    Logger getLogger();

    @SuppressWarnings("unchecked")
    default void channelActive0(ChannelHandlerContext ctx) throws Exception {
        // 新的channel激活时，绑定channel与session的关系
        Channel channel = ctx.channel();

        Session<TcpServer> session = new TcpClientSession(channel);
        getSessionService().addSession(session);

        getLogger().debug("服务器接收到客户端的连接，客户端ip：{}", channel.remoteAddress());

        channelRegistered(ctx);
    }

    @SneakyThrows
    default void channelRead1(ChannelHandlerContext cx, TcpPacket packet, Request.Protocol protocol) {
        TcpClientSession session = (TcpClientSession) getSessionService().getSessionById(cx.channel().id().toString());
        if (session == null) {
            getLogger().error("channelRead失败，channel对应的session为null");
            return;
        }

        if (MessageCmd.PING.equals(packet.getCmd())) {
            getLogger().debug("收到ping");
            return;
        } else if (MessageCmd.LOGIN.equals(packet.getCmd())) {
            Object userId = packet.getPayload();
            if (userId == null) {
                getLogger().error("处理LOGIN请求包时，content内容为null。忽略此包。");
                return;
            }
            String userIdStr;
            if (Request.Protocol.TCP == protocol) {
                userIdStr = new String((byte[]) userId, UTF_8);
            } else if (Request.Protocol.WEBSOCKET == protocol) {
                userIdStr = (String) userId;
            } else {
                throw new UnsupportedOperationException("不支持的userId请求包的请求协议：" + protocol);
            }

//            getSessionManager().setUserId(session, userIdStr););
            packet.setPayload("绑定此通道的userId成功！");

            if (Request.Protocol.TCP == protocol) {
                session.sendMessage(packet);
            } else {
                String json = JsonUtil.DEFAULT_INSTANCE.writeValueAsString(packet);
//                session.sendMessage(new TextWebSocketFrame(json));
            }

            return;
        }

        Request bizRequest = new Request<>(packet, session, protocol);
        getRequestManager().addRequest(bizRequest);
    }

    default void userEventTriggered0(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent) {
            IdleState state = ((IdleStateEvent) evt).state();
            if (state == IdleState.ALL_IDLE) {
                getLogger().debug(
                    "channel超时没有读写操作，将主动关闭链接通道！session={}",
                    getSessionService().getSessionById(ctx.channel().id().toString()));
                ctx.close();
            }
        }
    }

    default void exceptionCaught0(ChannelHandlerContext ctx, Throwable cause) {
        if (!"远程主机强迫关闭了一个现有的连接。".equals(cause.getMessage())) {
            getLogger().error(cause.getMessage(), cause);
        }
        if (cause instanceof java.io.IOException) {
            getLogger().error(cause.getMessage());
            return;
        } else {
            getLogger().error(cause.getMessage(), cause);
        }
        ctx.close();
    }

    default void channelInactive0(ChannelHandlerContext ctx) {
        ctx.fireChannelInactive();
        Channel channel = ctx.channel();
        if (channel != null) {
            Session session = getSessionService().removeSession(ctx.channel().id().toString());
            getLogger().debug("成功关闭了一个websocket连接：session={}", session.toString());
        }

        // todo 分发用户下线事件
    }
}
