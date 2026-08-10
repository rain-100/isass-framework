// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.netty.websocket;

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
import io.netty.util.CharsetUtil;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import vip.isass.framework.net.netty.channel.ChannelEventHandler;
import vip.isass.framework.net.netty.packet.TcpPacket;
import vip.isass.framework.net.netty.request.Request;
import vip.isass.framework.net.netty.request.RequestManager;
import vip.isass.framework.net.core.session.ISessionService;
import vip.isass.framework.common.support.JsonUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Rain
 */
@Slf4j
@ChannelHandler.Sharable
public class WebsocketChannelEventHandler extends SimpleChannelInboundHandler<Object> implements ChannelEventHandler {

    private Map<Channel, WebSocketServerHandshaker> handshakers = new ConcurrentHashMap<>(128);

    private final ISessionService sessionService;

    @Getter
    private final RequestManager requestManager;

    public WebsocketChannelEventHandler(ISessionService sessionService, RequestManager requestManager) {
        this.sessionService = sessionService;
        this.requestManager = requestManager;
    }

    @Override
    public ISessionService getSessionService() {
        return sessionService;
    }

    @Override
    public Logger getLogger() {
        return log;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        channelActive0(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            handleHttpRequest(ctx, (FullHttpRequest) msg);
        } else if (msg instanceof WebSocketFrame) {
            handleWebsocketFrame(ctx, (WebSocketFrame) msg);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        ctx.fireUserEventTriggered(evt);
        userEventTriggered0(ctx, evt);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        exceptionCaught0(ctx, cause);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        channelInactive0(ctx);
    }

    @SneakyThrows
    private void handleWebsocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
        if (frame instanceof CloseWebSocketFrame) {
            handshakers.get(ctx.channel()).close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
            return;
        }

        if (frame instanceof PingWebSocketFrame) {
            ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
            return;
        }

        if (frame instanceof BinaryWebSocketFrame) {
            // todo deal binaryFrame
            return;
        }

        if (frame instanceof TextWebSocketFrame) {
            Channel channel = ctx.channel();
            String request = ((TextWebSocketFrame) frame).text();
            log.debug("接收到文本请求：{}", request);

            TcpPacket packet = JsonUtil.DEFAULT_INSTANCE.readValue(request, TcpPacket.class);
            channelRead1(ctx, packet, Request.Protocol.WEBSOCKET);
        }
    }

    private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) {
        // 如果HTTP解码失败，或者请求头没有websocket，则返回HTTP异常
        // req.decoderResult().isFailure()
        if (req.decoderResult().isFailure()
                || (!HttpHeaderValues.WEBSOCKET.toString().equals(req.headers().get(HttpHeaderNames.UPGRADE)))) {
            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
            return;
        }

        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
                "ws://localhost:8080/websocket",
                null,
                false);
        WebSocketServerHandshaker handshaker = wsFactory.newHandshaker(req);
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            handshakers.put(ctx.channel(), handshaker);
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
