// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.core;


import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import vip.isass.framework.net.core.allocator.AllocatorController;
import vip.isass.framework.net.core.allocator.AllocatorService;
import vip.isass.framework.net.core.handler.OnAliasEventHandler;
import vip.isass.framework.net.core.handler.OnClientP2pEventHandler;
import vip.isass.framework.net.core.handler.OnClientSendBroadcastEventHandler;
import vip.isass.framework.net.core.handler.OnDisconnectedBroadcastHandler;
import vip.isass.framework.net.core.handler.OnLoginEventHandler;
import vip.isass.framework.net.core.handler.OnLogoutEventHandler;
import vip.isass.framework.net.core.handler.OnPingEventHandler;
import vip.isass.framework.net.core.handler.manager.EventManager;
import vip.isass.framework.net.core.handler.manager.MessageEventRegisterManager;
import vip.isass.framework.net.core.server.ServerStartupManager;
import vip.isass.framework.net.core.server.allocator.INodeAllocatorService;
import vip.isass.framework.net.core.session.ISessionService;
import vip.isass.framework.net.core.session.LocalSessionService;
import vip.isass.framework.net.core.handler.OnConnectEventHandler;
import vip.isass.framework.net.core.handler.OnDisconnectEventHandler;
import vip.isass.framework.net.core.handler.OnErrorEventHandler;
import vip.isass.framework.net.core.handler.OnAnyMessageEventHandler;
import vip.isass.framework.net.core.handler.OnMessageEventHandler;
import vip.isass.framework.net.core.handler.IMessageEventRegister;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@AutoConfiguration
@Import({
        OnDisconnectedBroadcastHandler.class,
        OnLoginEventHandler.class,
        OnLogoutEventHandler.class,
        OnPingEventHandler.class,
        OnAliasEventHandler.class,
        OnClientP2pEventHandler.class,
        OnClientSendBroadcastEventHandler.class,
        ServerStartupManager.class,
        LocalSessionService.class
})
@ConditionalOnProperty(name = "kernel.net.enabled", havingValue = "true", matchIfMissing = false)
public class NetCoreAutoConfiguration {

    @Bean
    public EventManager eventManager(ISessionService sessionService,
                                     @Autowired(required = false) List<OnConnectEventHandler> onConnectEventHandlers,
                                     @Autowired(required = false) List<OnDisconnectEventHandler> onDisconnectEventHandlers,
                                     @Autowired(required = false) List<OnErrorEventHandler> onErrorEventHandlers,
                                     @Autowired(required = false) List<OnAnyMessageEventHandler<?>> onAnyMessageEventHandlers,
                                     @Autowired(required = false) List<OnMessageEventHandler<?>> onMessageEventHandlers) {
        return new EventManager(sessionService,
                onConnectEventHandlers,
                onDisconnectEventHandlers,
                onErrorEventHandlers,
                onAnyMessageEventHandlers,
                onMessageEventHandlers);
    }

    @Bean
    public MessageEventRegisterManager messageEventRegisterManager(
            @Autowired(required = false) List<IMessageEventRegister> messageEventRegisters,
            @Autowired(required = false) List<OnMessageEventHandler<?>> onMessageEventHandlers) {
        return new MessageEventRegisterManager(messageEventRegisters, onMessageEventHandlers);
    }

    @Bean
    public AllocatorService allocatorService(List<INodeAllocatorService> nodeAllocatorServices) {
        return new AllocatorService(nodeAllocatorServices);
    }

    @Bean
    public AllocatorController allocatorController(AllocatorService allocatorService) {
        return new AllocatorController(allocatorService);
    }

}
