// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.cache.redis;

import com.baomidou.lock.annotation.Lock4j;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.scheduling.annotation.Scheduled;
import vip.isass.framework.common.support.LocalDateTimeUtil;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class RedisStreamMessageCleaner {

    private static final String REMOVE_MESSAGE_LUA_SCRIPT = "return redis.call('XTRIM', KEYS[1], 'MINID', '~', ARGV[1])";
    private static final DefaultRedisScript<Long> REDIS_SCRIPT = new DefaultRedisScript<>(REMOVE_MESSAGE_LUA_SCRIPT, Long.class);

    private final RedisTemplate<String, ?> redisTemplate;

    private final List<IRemovableStreamMessageProvider> providers;

    public RedisStreamMessageCleaner(RedisTemplate<String, ?> redisTemplate,
                                      List<IRemovableStreamMessageProvider> providers) {
        this.redisTemplate = redisTemplate;
        this.providers = providers;
    }

    /**
     * 每隔5分钟删除一次5分钟前的旧消息
     */
    @SuppressWarnings("unchecked")
    @Scheduled(initialDelay = 5 * 60 * 1000, fixedDelay = 5 * 60 * 1000)
    @Lock4j(name = "removeStreamMessage", acquireTimeout = 0, expire = 30_000)
    public void process() {
        if (providers == null) {
            return;
        }

        /*
        https://redis.io/commands/xtrim/
        redis 要在6.2.0 及以上才有 MINID 策略
        Starting with Redis version 6.2.0: Added the MINID trimming strategy and the LIMIT option.
         */
        String fiveMinuteAgo = LocalDateTimeUtil.localDateTimeToEpochMilli(LocalDateTimeUtil.now().minusMinutes(5)) + "";
        providers.forEach(p -> {
            log.debug("cleanup redis stream old message. keys: {}", p.getKeys());
            try {
                redisTemplate.execute(
                        REDIS_SCRIPT,
                        RedisSerializer.string(),
                        (RedisSerializer<Long>) redisTemplate.getValueSerializer(),
                        p.getKeys() instanceof List ? (List<String>) p.getKeys() : new ArrayList<>(p.getKeys()),
                        fiveMinuteAgo);
            } catch (InvalidDataAccessApiUsageException e) {
                if (e.getMessage().contains("ERR syntax error")) {
                    log.warn("清理 redis stream message 错误！redis 版本需 >= 6.2.0");
                } else {
                    throw new RuntimeException(e);
                }
            }
        });
    }
}
