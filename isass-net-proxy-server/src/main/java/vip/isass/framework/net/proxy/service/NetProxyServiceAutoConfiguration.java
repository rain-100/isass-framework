// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.proxy.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import vip.isass.framework.net.core.handler.IMessageEventRegister;
import vip.isass.framework.net.core.session.ISessionService;
import vip.isass.framework.net.proxy.core.CmdRedisService;
import vip.isass.framework.net.proxy.service.controller.NetMessageSenderController;
import vip.isass.framework.net.proxy.service.controller.NetSessionController;
import vip.isass.framework.net.proxy.service.handler.net.MessageDispatcherHandler;
import vip.isass.framework.net.proxy.service.handler.redis.S2CMessageRedisSubscriber;
import vip.isass.framework.net.proxy.service.job.ProxyClientCmdListeningJob;
import vip.isass.framework.net.proxy.service.job.RemoveEarlyMessageJob;
import vip.isass.framework.net.proxy.service.service.GatewayToRedisMessageService;
import vip.isass.framework.net.proxy.service.service.ProxyClientCmdListeningService;
import vip.isass.framework.net.proxy.service.service.RemoveC2SMessageService;

import java.util.List;

@AutoConfiguration
@Import({
        NetMessageSenderController.class,
        NetSessionController.class
})
@EnableScheduling
@ConditionalOnProperty(name = {"kernel.net.enabled", "kernel.net.proxy.enabled"}, havingValue = "true")
public class NetProxyServiceAutoConfiguration {

    @Bean
    MessageDispatcherHandler messageDispatcherHandler(@Value("${spring.application.name:}") String applicationName, GatewayToRedisMessageService gatewayToRedisMessageService) {
        return new MessageDispatcherHandler(applicationName, gatewayToRedisMessageService);
    }

    @Bean
    S2CMessageRedisSubscriber s2cMessageRedisSubscriber(ISessionService sessionService) {
        return new S2CMessageRedisSubscriber(sessionService);
    }

    @Bean
    ProxyClientCmdListeningJob proxyClientCmdListeningJob(ProxyClientCmdListeningService proxyClientCmdListeningService) {
        return new ProxyClientCmdListeningJob(proxyClientCmdListeningService);
    }

    @Bean
    RemoveEarlyMessageJob removeEarlyMessageJob(RemoveC2SMessageService removeC2SMessageService) {
        return new RemoveEarlyMessageJob(removeC2SMessageService);
    }

    @Bean
    GatewayToRedisMessageService gatewayToRedisMessageService(RedisTemplate<String, Object> redisTemplate) {
        return new GatewayToRedisMessageService(redisTemplate);
    }

    @Bean
    ProxyClientCmdListeningService proxyClientCmdListeningService(@Value("${spring.application.name:}") String applicationName, CmdRedisService cmdRedisService, @Autowired(required = false) List<IMessageEventRegister> messageEventRegisters) {
        return new ProxyClientCmdListeningService(applicationName, cmdRedisService, messageEventRegisters);
    }

    @Bean
    RemoveC2SMessageService removeC2SMessageService(RedisTemplate<String, Object> redisTemplate) {
        return new RemoveC2SMessageService(redisTemplate);
    }

}
