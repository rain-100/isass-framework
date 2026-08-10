// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.websocket;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import vip.isass.framework.net.core.handler.manager.EventManager;
import vip.isass.framework.net.core.session.ISessionService;
import vip.isass.framework.net.websocket.allocator.WebsocketLocalNodeAllocatorService;
import vip.isass.framework.net.websocket.allocator.WebsocketNodeAllocatorConfiguration;
import vip.isass.framework.net.websocket.websocket.WebsocketChannelInboundHandler;
import vip.isass.framework.net.websocket.websocket.WebsocketChannelInitializerHandler;
import vip.isass.framework.net.websocket.websocket.WebsocketServer;

@AutoConfiguration
@Import({
        WebsocketProperties.class,
        WebsocketNodeAllocatorConfiguration.class,
        WebsocketLocalNodeAllocatorService.class
})
@ConditionalOnProperty(name = {"kernel.net.enabled", "kernel.net.websocket.enabled"}, havingValue = "true")
public class NetWebsocketAutoConfiguration {

    @Bean
    public WebsocketEventHandlerRegister websocketEventHandlerRegister() {
        return new WebsocketEventHandlerRegister();
    }

    @Bean
    public WebsocketChannelInboundHandler websocketChannelInboundHandler(
            ISessionService sessionService, EventManager eventManager) {
        return new WebsocketChannelInboundHandler(sessionService, eventManager);
    }

    @Bean
    public WebsocketChannelInitializerHandler websocketChannelInitializerHandler(
            WebsocketProperties websocketProperties,
            WebsocketChannelInboundHandler websocketChannelInboundHandler) {
        return new WebsocketChannelInitializerHandler(websocketProperties, websocketChannelInboundHandler);
    }

    @Bean
    public WebsocketServer websocketServer(
            WebsocketChannelInitializerHandler websocketChannelInitializerHandler,
            WebsocketProperties websocketProperties) {
        return new WebsocketServer(websocketChannelInitializerHandler, websocketProperties);
    }

}
