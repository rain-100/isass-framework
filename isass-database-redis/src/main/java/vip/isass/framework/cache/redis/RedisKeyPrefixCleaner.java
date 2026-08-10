// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.cache.redis;

import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;

import java.util.ArrayList;
import java.util.List;

/** Deletes a bounded Redis key namespace without scanning unrelated application keys. */
public class RedisKeyPrefixCleaner {

    private static final int SCAN_COUNT = 1_000;

    private final StringRedisTemplate redisTemplate;

    public RedisKeyPrefixCleaner(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public long unlinkByPrefix(String prefix) {
        long removed = 0L;
        List<String> keys = new ArrayList<>(SCAN_COUNT);
        ScanOptions options = ScanOptions.scanOptions().match(prefix + "*").count(SCAN_COUNT).build();
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                keys.add(cursor.next());
                if (keys.size() == SCAN_COUNT) {
                    removed += unlink(keys);
                    keys.clear();
                }
            }
        }
        return removed + unlink(keys);
    }

    private long unlink(List<String> keys) {
        if (keys.isEmpty()) return 0L;
        Long count = redisTemplate.unlink(keys);
        return count == null ? 0L : count;
    }
}
