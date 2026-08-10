// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.websocket.websocket;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;
import vip.isass.framework.net.core.server.NetProtocol;
import vip.isass.framework.net.core.server.Server;
import vip.isass.framework.net.websocket.WebsocketProperties;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * @author Rain
 */
@Slf4j
public class WebsocketServer implements Server {

    private final WebsocketChannelInitializerHandler websocketChannelInitializerHandler;

    private final WebsocketProperties websocketProperties;

    private ExecutorService executorService;

    private EventLoopGroup boss;

    private EventLoopGroup worker;

    public WebsocketServer(WebsocketChannelInitializerHandler websocketChannelInitializerHandler,
                            WebsocketProperties websocketProperties) {
        this.websocketChannelInitializerHandler = websocketChannelInitializerHandler;
        this.websocketProperties = websocketProperties;
    }

    @Override
    public String getListeningAddress() {
        return websocketProperties.getHostName() + ":" + websocketProperties.getPort();
    }

    public void start() {
        if (executorService == null) {
            executorService = Executors.newSingleThreadExecutor(
                    new ThreadFactoryBuilder()
                            .setNameFormat("websocket-server-starter")
                            .setDaemon(true)
                            .build()
            );
        }

        executorService.execute(() -> {
            boss = new NioEventLoopGroup();
            worker = new NioEventLoopGroup();
            try {
                ServerBootstrap bootstrap = new ServerBootstrap();
                bootstrap.group(boss, worker)
                        .channel(NioServerSocketChannel.class)
                        .handler(new LoggingHandler(LogLevel.DEBUG))
                        .childHandler(websocketChannelInitializerHandler);

                ChannelFuture f = bootstrap.bind(websocketProperties.getHostName(), websocketProperties.getPort()).sync();
                f.channel().closeFuture().sync();
            } catch (InterruptedException e) {
                log.error("websocket 服务器启动失败！{}", e.getMessage(), e);
            } finally {
                boss.shutdownGracefully();
                worker.shutdownGracefully();
            }
        });
    }

    @Override
    public void stop() {
        if (boss != null) {
            boss.shutdownGracefully();
        }
        if (worker != null) {
            worker.shutdownGracefully();
        }
    }

    @Override
    public NetProtocol netProtocol() {
        return NetProtocol.websocket;
    }

}
