// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.netty;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import vip.isass.framework.net.core.session.ISessionService;
import vip.isass.framework.net.netty.config.NetProperties;
import vip.isass.framework.net.netty.packet.Decoder;
import vip.isass.framework.net.netty.packet.Encoder;
import vip.isass.framework.net.netty.packet.impl.coder.IsassBinaryPacketDecoder;
import vip.isass.framework.net.netty.packet.impl.coder.IsassBinaryPacketEncoder;
import vip.isass.framework.net.netty.request.RequestManager;
import vip.isass.framework.net.netty.request.handler.DefaultHttpRequestHandler;
import vip.isass.framework.net.netty.request.handler.RequestHandler;
import vip.isass.framework.net.netty.request.worker.Worker;
import vip.isass.framework.net.netty.request.worker.WorkerPool;
import vip.isass.framework.net.netty.request.worker.normal.DefaultWorker;
import vip.isass.framework.net.netty.request.worker.normal.DefaultWorkerPool;
import vip.isass.framework.net.netty.tcp.TcpChannelEventHandler;
import vip.isass.framework.net.netty.tcp.TcpChannelInitializerHandler;
import vip.isass.framework.net.netty.tcp.TcpServer;
import vip.isass.framework.net.netty.websocket.WebsocketChannelEventHandler;
import vip.isass.framework.net.netty.websocket.WebsocketChannelInitializerHandler;
import vip.isass.framework.net.netty.websocket.WebsocketServer;

@AutoConfiguration
@Import({
        NetProperties.class,
        TcpServer.class
})
@ConditionalOnProperty(name = {"kernel.net.enabled", "kernel.net.netty.enabled"}, havingValue = "true", matchIfMissing = false)
public class NettyAutoConfiguration {

    // ==================== WebSocket beans ====================

    @Bean
    @ConditionalOnMissingBean(Encoder.class)
    public IsassBinaryPacketEncoder isassBinaryPacketEncoder() {
        return new IsassBinaryPacketEncoder();
    }

    @Bean
    @ConditionalOnMissingBean(Decoder.class)
    public IsassBinaryPacketDecoder isassBinaryPacketDecoder() {
        return new IsassBinaryPacketDecoder();
    }

    @Bean
    @ConditionalOnMissingBean(RestTemplate.class)
    @Primary
    public RestTemplate restTemplate(NetProperties netProperties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(netProperties.getRestTemplateTimeOut());
        requestFactory.setReadTimeout(netProperties.getRestTemplateTimeOut());
        return new RestTemplate(requestFactory);
    }

    @Bean
    @ConditionalOnMissingBean(RequestHandler.class)
    public DefaultHttpRequestHandler defaultHttpRequestHandler(NetProperties netProperties, RestTemplate restTemplate) {
        return new DefaultHttpRequestHandler(netProperties, restTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(WorkerPool.class)
    public DefaultWorkerPool defaultWorkerPool() {
        return new DefaultWorkerPool();
    }

    @Bean
    @ConditionalOnMissingBean(Worker.class)
    @Scope("prototype")
    public DefaultWorker defaultWorker(RequestHandler requestHandler) {
        return new DefaultWorker(requestHandler);
    }

    @Bean
    public RequestManager requestManager(WorkerPool workerPool) {
        return new RequestManager(workerPool);
    }

    @Bean
    @ConditionalOnMissingBean(WebsocketChannelEventHandler.class)
    public WebsocketChannelEventHandler websocketChannelEventHandler(
            ISessionService sessionService,
            RequestManager requestManager) {
        return new WebsocketChannelEventHandler(sessionService, requestManager);
    }

    @Bean
    @ConditionalOnMissingBean(WebsocketChannelInitializerHandler.class)
    @Primary
    public WebsocketChannelInitializerHandler websocketChannelInitializerHandler(
            WebsocketChannelEventHandler channelEventHandler,
            @Value("${server.websocket.timeout:240000}") int timeout) {
        return new WebsocketChannelInitializerHandler(channelEventHandler, timeout);
    }

    @Bean
    @ConditionalOnMissingBean(WebsocketServer.class)
    public WebsocketServer websocketServer(
            WebsocketChannelInitializerHandler channelInitializerHandler,
            @Value("${server.websocket.port:20003}") int port) {
        return new WebsocketServer(channelInitializerHandler, port);
    }

    // ==================== TCP beans (conditional) ====================

    @Bean
    @ConditionalOnMissingBean(TcpChannelEventHandler.class)
    @ConditionalOnProperty(prefix = "core-net.tcp", name = "enabled", havingValue = "true")
    public TcpChannelEventHandler tcpChannelEventHandler(
            ISessionService sessionService,
            RequestManager requestManager) {
        return new TcpChannelEventHandler(sessionService, requestManager);
    }

    @Bean
    @ConditionalOnMissingBean(TcpChannelInitializerHandler.class)
    @ConditionalOnProperty(prefix = "core-net.tcp", name = "enabled", havingValue = "true")
    public TcpChannelInitializerHandler tcpChannelInitializerHandler(
            Encoder encoder,
            TcpChannelEventHandler channelEventHandler,
            @Value("${tcp.server.socket.timeout:120000}") int timeout) {
        return new TcpChannelInitializerHandler(encoder, channelEventHandler, timeout);
    }
}
