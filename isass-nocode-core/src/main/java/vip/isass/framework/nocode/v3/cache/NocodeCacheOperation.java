package vip.isass.framework.nocode.v3.cache;

import java.util.Objects;

/**
 * Cache metadata attached to a nocode v3 operation.
 *
 * @param mode      cache behavior
 * @param cacheName cache name
 * @param key       cache key or key descriptor
 */
public record NocodeCacheOperation(
        NocodeCacheMode mode,
        String cacheName,
        Object key
) {

    public NocodeCacheOperation {
        mode = Objects.requireNonNull(mode, "mode");
        cacheName = Objects.requireNonNull(cacheName, "cacheName");
        if (cacheName.isBlank()) {
            throw new IllegalArgumentException("cacheName must not be blank");
        }
    }
}
