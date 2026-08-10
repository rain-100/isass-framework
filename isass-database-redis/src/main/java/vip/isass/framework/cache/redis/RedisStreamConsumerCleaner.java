// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.cache.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.core.RedisTemplate;

import jakarta.annotation.PreDestroy;
import java.util.List;

@Slf4j
public class RedisStreamConsumerCleaner {

    private final RedisTemplate<String, ?> redisTemplate;

    private final List<IRedisStreamListener<?>> listeners;

    public RedisStreamConsumerCleaner(RedisTemplate<String, ?> redisTemplate,
                                      List<IRedisStreamListener<?>> listeners) {
        this.redisTemplate = redisTemplate;
        this.listeners = listeners;
    }

    @PreDestroy
    public <T> void destroy() {
        if (listeners == null) {
            return;
        }
        for (IRedisStreamListener<?> listener : listeners) {
            try {
                Consumer consumer = Consumer.from(listener.getConsumerGroup(), listener.getConsumerName());
                log.info("cleaning redis stream consumer[{}|{}]",
                        listener.getKey(),
                        consumer);
                redisTemplate.opsForStream().deleteConsumer(listener.getKey(), consumer);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }
}
