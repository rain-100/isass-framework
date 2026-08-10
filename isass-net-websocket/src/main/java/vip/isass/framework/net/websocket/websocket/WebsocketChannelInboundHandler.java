// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.websocket.websocket;

import cn.hutool.core.util.StrUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.CharsetUtil;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import vip.isass.framework.common.support.JsonUtil;
import vip.isass.framework.net.core.handler.manager.EventManager;
import vip.isass.framework.net.core.message.Message;
import vip.isass.framework.net.core.message.MessageCmd;
import vip.isass.framework.net.core.session.ISessionService;
import vip.isass.framework.net.websocket.packet.WebsocketPacket;
import vip.isass.framework.net.websocket.session.WebsocketClientSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Rain
 */
@Slf4j
@ChannelHandler.Sharable
public class WebsocketChannelInboundHandler extends SimpleChannelInboundHandler<Object> {

    private Map<Channel, WebSocketServerHandshaker> handshakers = new ConcurrentHashMap<>(128);

    @Getter
    private final ISessionService sessionService;

    private final EventManager eventManager;

    public WebsocketChannelInboundHandler(ISessionService sessionService, EventManager eventManager) {
        this.sessionService = sessionService;
        this.eventManager = eventManager;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // 新的channel激活时，绑定channel与session的关系
        Channel channel = ctx.channel();
        WebsocketClientSession session = new WebsocketClientSession(channel);
        eventManager.onConnect(session);
        channelRegistered(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof FullHttpRequest) {
            handleHttpRequest(ctx, (FullHttpRequest) msg);
        } else if (msg instanceof WebSocketFrame) {
            handleWebsocketFrame(ctx, (WebSocketFrame) msg);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        ctx.fireUserEventTriggered(evt);
        if (evt instanceof IdleStateEvent) {
            IdleState state = ((IdleStateEvent) evt).state();
            if (state == IdleState.ALL_IDLE) {
                log.debug(
                        "channel超时没有读写操作，将主动关闭链接通道！sessionId[{}]",
                        ctx.channel().id().toString());
                ctx.close();
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Channel channel = ctx.channel();
        WebsocketClientSession session = new WebsocketClientSession(channel);
        eventManager.onError(session, cause);
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        ctx.fireChannelInactive();
        Channel channel = ctx.channel();
        if (channel != null) {
            WebsocketClientSession session = new WebsocketClientSession(channel);
            eventManager.onDisconnect(session);
        }

        // todo 分发用户下线事件
    }

    @SneakyThrows
    private void handleWebsocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
        if (frame instanceof CloseWebSocketFrame) {
            handshakers.get(ctx.channel())
                    .close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
            return;
        }

        if (frame instanceof PingWebSocketFrame) {
            ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
            return;
        }

        if (frame instanceof BinaryWebSocketFrame) {
            WebsocketClientSession session = new WebsocketClientSession(ctx.channel());
            session.sendMessage(MessageCmd.ERROR, "暂不支持二进制帧");
            return;
        }

        if (frame instanceof TextWebSocketFrame) {
            Channel channel = ctx.channel();
            String request = ((TextWebSocketFrame) frame).text();
            log.trace("接收到文本请求：{}", request);

            WebsocketPacket packet = JsonUtil.DEFAULT_INSTANCE.readValue(request, WebsocketPacket.class);
            WebsocketClientSession session = new WebsocketClientSession(channel);
            eventManager.onMessage(
                    Message.builder()
                            .senderSessionId(session.getSessionId())
                            .senderSession(session)
                            .cmd(packet.getCmd())
                            .payload(packet.getPayload())
                            .build());
        }
    }

    private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) {
        // 如果HTTP解码失败，或者请求头没有websocket，则返回HTTP异常
        // 如果消息头中没有包含Upgrade字段或者它的值不是websocket，则返回Http 400响应
        if (req.decoderResult().isFailure()
                || (!HttpHeaderValues.WEBSOCKET.toString().equals(req.headers().get(HttpHeaderNames.UPGRADE)))) {
            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
            return;
        }

        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
                StrUtil.format("ws://{}", req.headers().get(HttpHeaderNames.HOST)),
                null,
                false);
        WebSocketServerHandshaker handshaker = wsFactory.newHandshaker(req);
        if (handshaker == null) {
            // 不支持websocket协议
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            handshakers.put(ctx.channel(), handshaker);
            // 响应消息返回给客户端
            handshaker.handshake(ctx.channel(), req);
        }
    }

    private void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, DefaultFullHttpResponse resp) {
        if (resp.status().code() != HttpResponseStatus.OK.code()) {
            ByteBuf buf = Unpooled.copiedBuffer(resp.status().toString(), CharsetUtil.UTF_8);
            resp.content().writeBytes(buf);
            buf.release();
            setContentLength(resp, resp.content().readableBytes());
        }

        ChannelFuture channelFuture = ctx.channel().writeAndFlush(resp);
        if (!isKeepAlive(req) || resp.status().code() != HttpResponseStatus.OK.code()) {
            channelFuture.addListener(ChannelFutureListener.CLOSE);
        }
    }

    private boolean isKeepAlive(FullHttpRequest req) {
        return HttpHeaderValues.KEEP_ALIVE.toString().equals(req.headers().get(HttpHeaderNames.CONNECTION));
    }

    private void setContentLength(DefaultFullHttpResponse resp, int length) {
        resp.headers().set(HttpHeaderNames.CONTENT_LENGTH, length);
    }

}
