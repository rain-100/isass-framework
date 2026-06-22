package vip.isass.framework.net.netty;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import vip.isass.framework.net.core.session.ISessionService;
import vip.isass.framework.net.netty.packet.Decoder;
import vip.isass.framework.net.netty.packet.Encoder;
import vip.isass.framework.net.netty.request.handler.DefaultHttpRequestHandler;
import vip.isass.framework.net.netty.request.handler.RequestHandler;
import vip.isass.framework.net.netty.request.worker.WorkerPool;
import vip.isass.framework.net.netty.tcp.TcpChannelEventHandler;
import vip.isass.framework.net.netty.tcp.TcpChannelInitializerHandler;
import vip.isass.framework.net.netty.tcp.TcpServer;
import vip.isass.framework.net.netty.websocket.WebsocketServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class NettyAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(ISessionService.class, () -> mock(ISessionService.class))
            .withBean(WorkerPool.class, () -> mock(WorkerPool.class))
            .withBean(WebsocketServer.class, () -> mock(WebsocketServer.class))
            .withConfiguration(AutoConfigurations.of(NettyAutoConfiguration.class))
            .withPropertyValues(
                    "kernel.net.enabled=true",
                    "kernel.net.netty.enabled=true"
            );

    @Test
    void defaultBeansAreRegistered() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(DefaultHttpRequestHandler.class);
            assertThat(context).hasSingleBean(Encoder.class);
            assertThat(context).hasSingleBean(Decoder.class);
        });
    }

    @Test
    void overrideRequestHandlerReplacesDefault() {
        contextRunner
                .withBean("customRequestHandler", RequestHandler.class, () -> mock(RequestHandler.class))
                .run(context -> {
                    assertThat(context).doesNotHaveBean(DefaultHttpRequestHandler.class);
                    assertThat(context).hasSingleBean(RequestHandler.class);
                });
    }

    @Test
    void tcpBeansCreatedWhenTcpEnabled() {
        contextRunner
                .withPropertyValues("core-net.tcp.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(TcpChannelEventHandler.class);
                    assertThat(context).hasSingleBean(TcpChannelInitializerHandler.class);
                    assertThat(context).hasSingleBean(TcpServer.class);
                });
    }
}
