// SPDX-License-Identifier: LGPL-3.0-only
package vip.isass.framework.net.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NioEventLoopTransportTest {
    @Test
    @Timeout(10)
    void bindsNioServerChannelAndAcceptsConnectionsWithTheNewEventLoop() throws Exception {
        EventLoopGroup boss = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
        EventLoopGroup worker = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
        CountDownLatch accepted = new CountDownLatch(1);
        Channel server = null;
        try {
            server = new ServerBootstrap()
                    .group(boss, worker)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel channel) {
                            accepted.countDown();
                            channel.close();
                        }
                    })
                    .bind("127.0.0.1", 0)
                    .sync()
                    .channel();
            try (Socket client = new Socket()) {
                client.connect((InetSocketAddress) server.localAddress(), 3_000);
                assertTrue(accepted.await(3, TimeUnit.SECONDS), "NIO worker must accept the client connection");
            }
        } finally {
            if (server != null) {
                server.close().syncUninterruptibly();
            }
            boss.shutdownGracefully(0, 3, TimeUnit.SECONDS);
            worker.shutdownGracefully(0, 3, TimeUnit.SECONDS);
            assertTrue(boss.terminationFuture().await(3, TimeUnit.SECONDS), "boss must terminate");
            assertTrue(worker.terminationFuture().await(3, TimeUnit.SECONDS), "worker must terminate");
        }
    }
}
