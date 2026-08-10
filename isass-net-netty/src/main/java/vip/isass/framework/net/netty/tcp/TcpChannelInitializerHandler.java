// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.netty.tcp;

import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.Getter;
import vip.isass.framework.net.netty.channel.ChannelEventHandler;
import vip.isass.framework.net.netty.channel.ChannelInitializerHandler;
import vip.isass.framework.net.netty.packet.Decoder;
import vip.isass.framework.net.netty.packet.Encoder;
import vip.isass.framework.net.netty.packet.impl.coder.IsassBinaryPacketDecoder;
import vip.isass.framework.common.support.BeanProviderUtil;

import java.util.concurrent.TimeUnit;

public class TcpChannelInitializerHandler extends ChannelInitializerHandler {

    @Getter
    private final int timeout;

    private final Encoder encoder;
    private final ChannelEventHandler channelEventHandler;

    public TcpChannelInitializerHandler(Encoder encoder, ChannelEventHandler channelEventHandler, int timeout) {
        this.encoder = encoder;
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

        // 添加解码器
        Decoder decoder = BeanProviderUtil.isInitialized() ? BeanProviderUtil.getBean(Decoder.class) : new IsassBinaryPacketDecoder();
        pipeline.addLast("decoder", decoder);

        // 添加事件的处理方法
        pipeline.addLast(channelEventHandler);

        // 添加编码器
        pipeline.addLast("encoder", encoder);

    }
}
