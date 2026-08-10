// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.netty.websocket;

import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import vip.isass.framework.net.netty.channel.ChannelEventHandler;
import vip.isass.framework.net.netty.channel.ChannelInitializerHandler;

import java.util.concurrent.TimeUnit;

/**
 * 客户端成功connect后执行此类来初始化化此channel的行为
 *
 * @author Rain
 */
@Slf4j
public class WebsocketChannelInitializerHandler extends ChannelInitializerHandler {

    /**
     * 默认4分钟
     */
    @Getter
    private final int timeout;

    private final ChannelEventHandler channelEventHandler;

    public WebsocketChannelInitializerHandler(ChannelEventHandler channelEventHandler, int timeout) {
        this.channelEventHandler = channelEventHandler;
        this.timeout = timeout;
    }

    @Override
    protected void initChannel(SocketChannel socketChannel) {
        ChannelPipeline pipeline = socketChannel.pipeline();

        // 设置tcp链路空闲超时时间
        pipeline.addLast(
            "idleStateHandler",
            new IdleStateHandler(0, 0, timeout, TimeUnit.MILLISECONDS));

        pipeline.addLast("http-codec", new HttpServerCodec());
        pipeline.addLast("aggregator", new HttpObjectAggregator(65536));
        pipeline.addLast("http-chunked", new ChunkedWriteHandler());
        pipeline.addLast(channelEventHandler);
    }
}