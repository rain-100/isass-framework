package vip.isass.framework.nocode.v3.cache;

import vip.isass.framework.nocode.v3.operation.NocodeOperation;
import vip.isass.framework.nocode.v3.operation.NocodeOperationInterceptor;
import vip.isass.framework.nocode.v3.operation.NocodeOperationInvoker;

import java.util.Objects;
import java.util.Optional;

/**
 * Applies nocode v3 cache metadata around an operation invocation.
 */
public class NocodeCacheInterceptor implements NocodeOperationInterceptor {

    private final NocodeCacheManager cacheManager;
    private final NocodeCacheOperationResolver cacheOperationResolver;
    private final NocodeCacheKeyGenerator keyGenerator;

    public NocodeCacheInterceptor(NocodeCacheManager cacheManager,
                                  NocodeCacheOperationResolver cacheOperationResolver,
                                  NocodeCacheKeyGenerator keyGenerator) {
        this.cacheManager = Objects.requireNonNull(cacheManager, "cacheManager");
        this.cacheOperationResolver = Objects.requireNonNull(cacheOperationResolver, "cacheOperationResolver");
        this.keyGenerator = Objects.requireNonNull(keyGenerator, "keyGenerator");
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> R intercept(NocodeOperation operation, NocodeOperationInvoker<R> next) {
        Optional<NocodeCacheOperation> cacheOperation = cacheOperationResolver.resolve(operation);
        if (cacheOperation.isEmpty()) {
            return next.invoke(operation);
        }

        NocodeCacheOperation metadata = cacheOperation.get();
        Optional<NocodeCache> cache = cacheManager.getCache(metadata.cacheName());
        if (cache.isEmpty()) {
            return next.invoke(operation);
        }

        Object key = cacheKey(operation, metadata);
        return switch (metadata.mode()) {
            case CACHEABLE -> (R) cacheable(operation, next, cache.get(), key);
            case PUT -> put(operation, next, cache.get(), key);
            case EVICT -> evict(operation, next, cache.get(), key);
        };
    }

    private Object cacheKey(NocodeOperation operation, NocodeCacheOperation metadata) {
        return metadata.key() == null ? keyGenerator.generate(operation) : metadata.key();
    }

    private <R> Object cacheable(NocodeOperation operation, NocodeOperationInvoker<R> next, NocodeCache cache, Object key) {
        Optional<Object> cachedValue = cache.get(key);
        if (cachedValue.isPresent()) {
            return cachedValue.get();
        }

        R result = next.invoke(operation);
        if (result != null) {
            cache.put(key, result);
        }
        return result;
    }

    private <R> R put(NocodeOperation operation, NocodeOperationInvoker<R> next, NocodeCache cache, Object key) {
        R result = next.invoke(operation);
        if (result != null) {
            cache.put(key, result);
        }
        return result;
    }

    private <R> R evict(NocodeOperation operation, NocodeOperationInvoker<R> next, NocodeCache cache, Object key) {
        R result = next.invoke(operation);
        cache.evict(key);
        return result;
    }
}
