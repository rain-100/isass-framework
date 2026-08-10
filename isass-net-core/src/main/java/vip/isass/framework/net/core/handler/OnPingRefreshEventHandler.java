// SPDX-License-Identifier: LGPL-3.0-only

// package vip.isass.framework.net.core.handler;
//
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.data.redis.core.RedisTemplate;
// import vip.isass.framework.net.core.message.Message;
// import vip.isass.framework.net.core.server.Server;
// import vip.isass.framework.net.core.session.LocalSessionService;
//
// import jakarta.annotation.Resource;
// import java.util.concurrent.ThreadLocalRandom;
// import java.util.concurrent.TimeUnit;
//
// /**
//  * ping 事件处理器
//  *
//  * @author rain
//  */
// @Slf4j
// @Configuration
// @ConditionalOnBean(Server.class)
// public class OnPingRefreshEventHandler extends OnPingEventHandler {
//
//     @Resource
//     private RedisTemplate<String, Object> redisTemplate;
//
//     @Override
//     public Object onMessage(Message message, String ping) {
//         redisTemplate.opsForValue().set(
//                 LocalSessionService.SESSION_REDIS_KEY + message.getSenderSessionId(),
//                 message.getSenderSessionId(),
//                 ThreadLocalRandom.current().nextInt(5, 10),
//                 TimeUnit.MINUTES);
//         log.debug("kernel:net:ping:sessionId:{}", message.getSenderSessionId());
//         return null;
//     }
//
// }
