// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.socketio;


import cn.hutool.core.util.StrUtil;
import com.corundumstudio.socketio.SocketConfig;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.SpringAnnotationScanner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.DefaultResourceLoader;
import vip.isass.framework.net.core.handler.manager.EventManager;
import vip.isass.framework.net.core.session.ISessionService;
import vip.isass.framework.net.socketio.allocator.SocketIoLocalNodeAllocatorService;
import vip.isass.framework.net.socketio.allocator.SocketioNodeAllocatorConfiguration;
import vip.isass.framework.net.socketio.handler.OnSocketIoConnectListener;
import vip.isass.framework.net.socketio.handler.OnSocketIoDisconnectListener;
import vip.isass.framework.net.socketio.handler.OnSocketIoErrorListener;

@Slf4j
@AutoConfiguration
@Import({
        SocketIoProperties.class,
        SocketioNodeAllocatorConfiguration.class,
        SocketIoLocalNodeAllocatorService.class,
        SocketioForwardController.class
})
@ConditionalOnProperty(name = {"kernel.net.enabled", "kernel.net.socketio.enabled"}, havingValue = "true")
public class SocketIoAutoConfiguration {

    @Bean
    public SocketIOServer socketIOServer(SocketIoProperties socketIoProperties,
                                          OnSocketIoErrorListener onErrorListener) {
        com.corundumstudio.socketio.Configuration config = new com.corundumstudio.socketio.Configuration();
        config.setHostname(socketIoProperties.getHostName());
        config.setPort(socketIoProperties.getPort());
        config.setMaxHttpContentLength(socketIoProperties.getMaxHttpContentLength());
        config.setMaxFramePayloadLength(socketIoProperties.getMaxFramePayloadLength());
        config.setBossThreads(1);
        config.setExceptionListener(onErrorListener);

        // ssl
        if (StrUtil.isNotBlank(socketIoProperties.getKeyStorePath())) {
            try {
                DefaultResourceLoader defaultResourceLoader = new DefaultResourceLoader();
                org.springframework.core.io.Resource resource = defaultResourceLoader.getResource(socketIoProperties.getKeyStorePath());
                if (resource.exists()) {
                    config.setKeyStore(resource.getInputStream());
                    config.setKeyStoreFormat(socketIoProperties.getKeyStoreFormat());
                    config.setKeyStorePassword(socketIoProperties.getKeyStorePassword());
                }
                config.setKeyStorePassword(socketIoProperties.getKeyStorePassword());
            } catch (Exception e) {
                log.error("socketio 加载证书失败", e);
            }
        }

        SocketConfig sockConfig = new SocketConfig();
        sockConfig.setReuseAddress(true);
        sockConfig.setTcpKeepAlive(false);
        config.setSocketConfig(sockConfig);

        return new SocketIOServer(config);
    }

    /**
     * 用于扫描 netty-socketio 的注解，比如 @OnConnect、@OnEvent
     **/
    @Bean
    public SpringAnnotationScanner springAnnotationScanner(SocketIOServer socketIoServer) {
        return new SpringAnnotationScanner(socketIoServer);
    }

    @Bean
    public OnSocketIoConnectListener onSocketIoConnectListener(EventManager eventManager) {
        return new OnSocketIoConnectListener(eventManager);
    }

    @Bean
    public OnSocketIoDisconnectListener onSocketIoDisconnectListener(ISessionService sessionService, EventManager eventManager) {
        return new OnSocketIoDisconnectListener(sessionService, eventManager);
    }

    @Bean
    public OnSocketIoErrorListener onSocketIoErrorListener(EventManager eventManager, ISessionService sessionService) {
        return new OnSocketIoErrorListener(eventManager, sessionService);
    }

    @Bean
    public SocketIoServer socketIoServer(SocketIOServer socketIOServer) {
        return new SocketIoServer(socketIOServer);
    }

    @Bean
    public SocketIoEventHandlerRegister socketIoEventHandlerRegister(SocketIoServer socketIoServer, ISessionService sessionService, EventManager eventManager) {
        return new SocketIoEventHandlerRegister(socketIoServer, sessionService, eventManager);
    }

}
