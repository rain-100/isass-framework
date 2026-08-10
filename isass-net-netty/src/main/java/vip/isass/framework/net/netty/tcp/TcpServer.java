// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.netty.tcp;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import vip.isass.framework.net.core.server.NetProtocol;
import vip.isass.framework.net.core.server.Server;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * @author Rain
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "core-net.tcp", name = "enabled", havingValue = "true")
public class TcpServer implements Server {

    private final TcpChannelInitializerHandler channelInitializerHandler;
    private final int port;

    private ExecutorService executorService;

    private ServerBootstrap bootstrap = null;

    private EventLoopGroup boss = null;

    private EventLoopGroup worker = null;

    private boolean isShutdown = true;

    public TcpServer(TcpChannelInitializerHandler channelInitializerHandler,
                     @Value("${core-net.tcp.port:20002}") int port) {
        this.channelInitializerHandler = channelInitializerHandler;
        this.port = port;
    }

    private synchronized boolean isShutdown() {
        return isShutdown;
    }

    private synchronized void setShutdown(boolean shutdown) {
        isShutdown = shutdown;
    }

    @Override
    public String getListeningAddress() {
        return bootstrap.config().localAddress().toString();
    }

    @Override
    public void start() {
        if (!isShutdown()) {
            return;
        }

        setShutdown(false);

        if (executorService == null) {
            executorService = Executors.newSingleThreadExecutor(
                    new ThreadFactoryBuilder()
                            .setNameFormat("netty-server")
                            .setDaemon(true)
                            .build()
            );
        }

        executorService.execute(() -> {
            boss = new NioEventLoopGroup();
            worker = new NioEventLoopGroup();
            try {
                bootstrap = new ServerBootstrap();

                bootstrap.group(boss, worker)
                        .channel(NioServerSocketChannel.class)

                        // BACKLOG用于构造服务端套接字ServerSocket对象，标识当服务器请求处理线程全满时，
                        // 用于临时存放已完成三次握手的请求的队列的最大长度。如果未设置或所设置的值小于1，Java将使用默认值50。
                        .option(ChannelOption.SO_BACKLOG, 1024)

                        // 在TCP/IP协议中，无论发送多少数据，总是要在数据前面加上协议头，同时，对方接收到数据，也需要发送ACK表示确认。
                        // 为了尽可能的利用网络带宽，TCP总是希望尽可能的发送足够大的数据。
                        // 这里就涉及到一个名为Nagle的算法，该算法的目的就是为了尽可能发送大块数据，避免网络中充斥着许多小数据块。
                        // TCP_NODELAY就是用于启用或关于Nagle算法。如果要求高实时性，有数据发送时就马上发送，
                        // 就将该选项设置为true关闭Nagle算法；
                        // 如果要减少发送次数减少网络交互，就设置为false等累积一定大小后再发送。默认为false。
                        .option(ChannelOption.TCP_NODELAY, true)

                        // 1 发现问题　　
                        // 　　我在开发一个socket服务器程序并反复调试的时候，发现了一个让人无比心烦的情况：
                        //    每次kill掉该服务器进程并重新启动的时候，都会出现bind错误：
                        //    error:98，Address already in use。然而再kill掉该进程，再次重新启动的时候，就bind成功了。
                        //    真让人摸不着头脑。难道一定要尝试两次才显得真诚？这不科学！
                        // 　　我的第一反应是kill进程的时候，并没有完全释放掉socket资源，倒致第二次启动的时候，bind失败。那么第三次怎么又成功了呢？
                        // 　　查资料：有人说是TIME_WAIT在捣鬼。
                        // 　　回想一下，Linux下的TIME_WAIT大概是2分钟，这样也合情合理。那么没有释放掉的资源是什么呢，是端口吗？
                        //    机智的我立刻决定做实验找出答案。启动服务器程序，在与客户建立连接之后，kill掉服务器。
                        //    飞快地在terminal里输入命令：netstat -an|grep 9877。这里9877是我服务器打算绑定的端口。果然：
                        // 　　结果显示9877端口正在被使用，并处于TCP中的TIME_WAIT状态。
                        //    再过两分钟，我再执行命令netstat -an|grep 9877，世界清静了，什么都没有。
                        // 　　终于找到了答案：果然是TIME_WAIT在捣鬼。
                        // 2 解决问题
                        // 　　问题找到了，可是怎么解决问题呢。如何才能结束掉这个TIME_WAIT状态呢？
                        //    否则每次调试之后，都要巴巴地等上两分钟，再进行下次调试。这太蠢了！想了好久，也没想出解决办法。
                        //    那TCP中有没有能关闭掉TIME_WAIT的选项呢？翻书！UNP中第7章就是讲socket选项的。还真没有找到。
                        //    但是，我找到了SO_REUSEADDR选项。关于此选项，书上说可以起到以下4个不同的功用：
                        // 　　（1）SO_REUSEADDR允许启动一个监听服务器并捆绑其众所周知的端口，即使以前建立的将该端口用作他们的本地端口的连接仍存在。
                        // 　　（2）允许在同一端口上启动同一服务器的多个实例，只要每个实例捆绑一个不同的本地IP地址即可。
                        // 　　（3）SO_REUSEADDR 允许单个进程捆绑同一端口到多个套接字上，只要每次捆绑指定不同的本地IP地址即可。
                        // 　　（4）SO_REUSEADDR允许完全重复的捆绑：当一个IP地址和端口号已绑定到某个套接字上时，如果传输协议支持，
                        //         同样的IP地址和端口还可以捆绑到另一个套接字上。一般来说本特性仅支持UDP套接字。
                        // 　　我遇到的情况正好符合情况1，并且书上说了：“所有TCP服务器都应该指定本套接字选项，
                        //    以允许服务器在这种情形下被重新启动。”那么试试看喽。
                        // 3 察漏补缺
                        // 　　既然TIME_WAIT这么讨厌，那它的存在有什么意义呢？毕竟服务器端已经中断掉连接了呀。
                        //    记得之前在看UNP的时候，上面好像有提到过，继续翻书：
                        // 　　书上说，TIME_WAIT状态有两个存在的理由：
                        // 　　（1）可靠地实现TCP全双工连接的终止；
                        // 　　（2）允许老的重复分节在网络中消逝。
                        //     解释：
                        // 　　（1）如果服务器最后发送的ACK因为某种原因丢失了，那么客户一定会重新发送FIN，
                        //         这样因为有TIME_WAIT的存在，服务器会重新发送ACK给客户，如果没有TIME_WAIT，
                        //         那么无论客户有没有收到ACK，服务器都已经关掉连接了，此时客户重新发送FIN，服务器将不会发送ACK，
                        //         而是RST，从而使客户端报错。也就是说，TIME_WAIT有助于可靠地实现TCP全双工连接的终止。
                        // 　　（2）如果没有TIME_WAIT，我们可以在最后一个ACK还未到达客户的时候，就建立一个新的连接。
                        //         那么此时，如果客户收到了这个ACK的话，就乱套了，必须保证这个ACK完全死掉之后，才能建立新的连接。
                        //         也就是说，TIME_WAIT允许老的重复分节在网络中消逝。
                        .childOption(ChannelOption.SO_REUSEADDR, true)

                        // 系统缓冲区的大小
                        .childOption(ChannelOption.SO_RCVBUF, 65536)
                        .childOption(ChannelOption.SO_SNDBUF, 65536)
                        .handler(new LoggingHandler(LogLevel.DEBUG))
                        .childHandler(channelInitializerHandler);
                // handler在初始化时就会执行，而childHandler会在客户端成功connect后才执行，这是两者的区别。

                ChannelFuture f = bootstrap.bind(port).sync();
                log.info("tcp 服务器启动成功！监听端口：{}", port);
                f.channel().closeFuture().sync();
            } catch (InterruptedException e) {
                log.error("tcp 服务器启动失败！{}", e.getMessage(), e);
            } finally {
                boss.shutdownGracefully();
                worker.shutdownGracefully();
                setShutdown(true);
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
        return NetProtocol.tcp;
    }

}
