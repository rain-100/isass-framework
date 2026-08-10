// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.proxy.upstream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.scheduling.annotation.EnableScheduling;
import vip.isass.framework.net.core.handler.OnMessageEventHandler;
import vip.isass.framework.net.core.handler.manager.IEventManager;
import vip.isass.framework.net.core.message.Message;
import vip.isass.framework.net.proxy.core.CmdRedisService;
import vip.isass.framework.net.proxy.upstream.cmd.CmdCollectJob;
import vip.isass.framework.net.proxy.upstream.cmd.CmdCollectService;

import java.util.List;

@AutoConfiguration
@EnableScheduling
@ConditionalOnProperty(name = {"kernel.net.enabled", "kernel.net.proxy.enabled"}, havingValue = "true")
public class NetProxyClientAutoConfiguration {

    @Bean
    CmdCollectJob cmdCollectJob(CmdCollectService svc) {
        return new CmdCollectJob(svc);
    }

    @Bean
    CmdCollectService cmdCollectService(CmdRedisService cmdRedis,
                                        @Autowired(required = false) List<OnMessageEventHandler<?>> regs,
                                        @Value("${spring.application.name:}") String appName) {
        return new CmdCollectService(cmdRedis, regs, appName);
    }

    @Bean
    MessageRedisStreamListener messageRedisStreamListener(IEventManager eventManager,
                                                          RedisTemplate<String, Object> rt,
                                                          @Value("${spring.application.name:}") String appName) {
        return new MessageRedisStreamListener(eventManager, rt, appName);
    }

    @Bean
    SessionServiceClientProxy sessionServiceClientProxy(@Value("${kernel.net.defaultProtocol:}") String proto,
                                                        RedisTemplate<String, Object> rt) {
        return new SessionServiceClientProxy(proto, rt);
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    StreamMessageListenerContainer<String, ObjectRecord<String, Message>>
    netTransferMessageListenerContainer(RedisConnectionFactory factory, MessageRedisStreamListener listener) {
        return listener.netTransferMessageListenerContainer(factory);
    }
}
