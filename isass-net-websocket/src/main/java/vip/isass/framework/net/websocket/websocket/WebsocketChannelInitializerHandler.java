// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.websocket.websocket;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketFrameAggregator;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import vip.isass.framework.net.websocket.WebsocketProperties;

import java.util.concurrent.TimeUnit;

/**
 * 客户端成功connect后执行此类来初始化化此channel的行为
 *
 * @author Rain
 */
@Slf4j
// 本实例是线程安全的可以被多个 channel 复用
@ChannelHandler.Sharable
public class WebsocketChannelInitializerHandler extends ChannelInitializer<SocketChannel> {

    /**
     * 默认4分钟
     */
    private final WebsocketProperties websocketProperties;

    private final WebsocketChannelInboundHandler websocketChannelInboundHandler;

    public WebsocketChannelInitializerHandler(WebsocketProperties websocketProperties,
                                               WebsocketChannelInboundHandler websocketChannelInboundHandler) {
        this.websocketProperties = websocketProperties;
        this.websocketChannelInboundHandler = websocketChannelInboundHandler;
    }

    @Override
    protected void initChannel(SocketChannel socketChannel) {
        ChannelPipeline pipeline = socketChannel.pipeline();

        // 设置tcp链路空闲超时时间
        pipeline.addLast(
                "idleStateHandler",
                new IdleStateHandler(0, 0, websocketProperties.getTimeout(), TimeUnit.MILLISECONDS));

        // 将请求和应答消息编码或者解码为HTTP消息
        pipeline.addLast("http-codec", new HttpServerCodec());
        // 将HTTP消息的多个部分组合成一条完整的HTTP消息
        pipeline.addLast("aggregator", new HttpObjectAggregator(websocketProperties.getAggregator()));
        // 将分片的 WebSocketFrame 聚合成完整的 FullWebSocketFrame, maxContentLength 设置为10M
        pipeline.addLast("FrameAggregator", new WebSocketFrameAggregator(websocketProperties.getMaxFramePayloadLength()));
        // 用来向客户端发送HTML5文件，主要用于支持浏览器和服务端进行WebSocket通信
        pipeline.addLast("http-chunked", new ChunkedWriteHandler());
        pipeline.addLast(websocketChannelInboundHandler);
    }
}