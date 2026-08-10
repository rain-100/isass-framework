// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.proxy.service.handler.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.Topic;
import vip.isass.framework.cache.redis.IRedisSubscriber;
import vip.isass.framework.net.core.NetRedisKey;
import vip.isass.framework.net.core.session.ISessionService;

/**
 * 服务端推送消息给客户端时，使用 redis 的 pubsub 功能中转消息
 * redis subscriber
 * 监听的 key 为 {@link vip.isass.framework.net.core.NetRedisKey#REDIS_PUBSUB_KEY}
 *
 * @author rain
 */
@Slf4j
public class S2CMessageRedisSubscriber implements IRedisSubscriber<vip.isass.framework.net.core.message.Message> {

    public final ISessionService sessionService;

    public S2CMessageRedisSubscriber(ISessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Override
    public void onMessage(vip.isass.framework.net.core.message.Message message, Message redisMessage, byte[] pattern) {
        log.trace("收到redis的s2c消息[{}]", message);
        sessionService.sendMessage(message);
    }

    @Override
    public Topic topic() {
        return new ChannelTopic(NetRedisKey.REDIS_PUBSUB_KEY);
    }

}
