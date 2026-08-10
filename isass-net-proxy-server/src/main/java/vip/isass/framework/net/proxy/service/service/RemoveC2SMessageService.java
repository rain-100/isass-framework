// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.proxy.service.service;

import com.baomidou.lock.annotation.Lock4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.RedisSerializer;
import vip.isass.framework.common.support.LocalDateTimeUtil;
import vip.isass.framework.net.core.NetRedisKey;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 删除旧的消息
 *
 * @author rain
 */
public class RemoveC2SMessageService {

    private final RedisTemplate<String, Object> redisTemplate;

    public RemoveC2SMessageService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 每隔5分钟删除一次5分钟前的旧消息
     */
    @SuppressWarnings("unchecked")
    @Lock4j(name = "removeEarlyMessage", acquireTimeout = 0, expire = 30_000)
    public void process() {
        Set<String> keys = scanKeys();
        if (keys == null) {
            return;
        }

        /*
        https://redis.io/commands/xtrim/
        redis 要在6.2.0 及以上才有 MINID 策略
        Starting with Redis version 6.2.0: Added the MINID trimming strategy and the LIMIT option.
         */
        String luaScript = "return redis.call('XTRIM', KEYS[1], 'MINID', '~', ARGV[1])";
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(luaScript, Long.class);
        String fiveMinuteAgo = LocalDateTimeUtil.localDateTimeToEpochMilli(LocalDateTimeUtil.now().minusMinutes(5)) + "";
        keys.forEach(k -> redisTemplate.execute(
                redisScript,
                RedisSerializer.string(),
                (RedisSerializer<Long>) redisTemplate.getValueSerializer(),
                Collections.singletonList(k),
                fiveMinuteAgo));
    }

    public Set<String> scanKeys() {
        RedisSerializer<?> keySerializer = redisTemplate.getKeySerializer();
        return redisTemplate.execute((RedisCallback<Set<String>>) connection -> {
            Set<String> keys = new HashSet<>();
            try (Cursor<byte[]> cursor = connection.scan(
                    ScanOptions.scanOptions()
                            .match(NetRedisKey.REDIS_STREAM_PREFIX_KEY.concat("*"))
                            .count(1000)
                            .build())) {
                while (cursor.hasNext()) {
                    Object key = keySerializer.deserialize(cursor.next());
                    if (key != null) {
                        keys.add(key.toString());
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return keys;
        });
    }

}
