package vip.isass.framework.nocode.v3.cache;

import org.junit.jupiter.api.Test;
import vip.isass.framework.nocode.v3.operation.NocodeOperation;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class NocodeCacheInterceptorTest {

    private final NocodeOperation operation = new NocodeOperation("attachment", "getById", Map.of("id", 1), String.class);
    private final MemoryCache cache = new MemoryCache();
    private final NocodeCacheManager cacheManager = cacheName -> "attachment".equals(cacheName)
            ? Optional.of(cache)
            : Optional.empty();
    private final NocodeCacheKeyGenerator keyGenerator = currentOperation -> currentOperation.arguments().get("id");

    @Test
    void cacheableReturnsCachedValueWithoutInvokingNext() {
        cache.put(1, "cached");
        NocodeCacheInterceptor interceptor = new NocodeCacheInterceptor(
                cacheManager,
                currentOperation -> Optional.of(new NocodeCacheOperation(NocodeCacheMode.CACHEABLE, "attachment", null)),
                keyGenerator
        );
        AtomicInteger invocations = new AtomicInteger();

        String result = interceptor.intercept(operation, currentOperation -> {
            invocations.incrementAndGet();
            return "loaded";
        });

        assertThat(result).isEqualTo("cached");
        assertThat(invocations).hasValue(0);
    }

    @Test
    void cacheableStoresLoadedValueWhenCacheMisses() {
        NocodeCacheInterceptor interceptor = new NocodeCacheInterceptor(
                cacheManager,
                currentOperation -> Optional.of(new NocodeCacheOperation(NocodeCacheMode.CACHEABLE, "attachment", null)),
                keyGenerator
        );

        String result = interceptor.intercept(operation, currentOperation -> "loaded");

        assertThat(result).isEqualTo("loaded");
        assertThat(cache.get(1)).contains("loaded");
    }

    @Test
    void putAlwaysInvokesNextAndUpdatesCache() {
        cache.put(1, "old");
        NocodeCacheInterceptor interceptor = new NocodeCacheInterceptor(
                cacheManager,
                currentOperation -> Optional.of(new NocodeCacheOperation(NocodeCacheMode.PUT, "attachment", null)),
                keyGenerator
        );

        String result = interceptor.intercept(operation, currentOperation -> "new");

        assertThat(result).isEqualTo("new");
        assertThat(cache.get(1)).contains("new");
    }

    @Test
    void evictRemovesCacheAfterSuccessfulInvocation() {
        cache.put(1, "old");
        NocodeCacheInterceptor interceptor = new NocodeCacheInterceptor(
                cacheManager,
                currentOperation -> Optional.of(new NocodeCacheOperation(NocodeCacheMode.EVICT, "attachment", null)),
                keyGenerator
        );

        String result = interceptor.intercept(operation, currentOperation -> "deleted");

        assertThat(result).isEqualTo("deleted");
        assertThat(cache.get(1)).isEmpty();
    }

    @Test
    void skipsCacheWhenResolverDoesNotMatchOperation() {
        NocodeCacheInterceptor interceptor = new NocodeCacheInterceptor(
                cacheManager,
                currentOperation -> Optional.empty(),
                keyGenerator
        );

        String result = interceptor.intercept(operation, currentOperation -> "loaded");

        assertThat(result).isEqualTo("loaded");
        assertThat(cache.get(1)).isEmpty();
    }

    static class MemoryCache implements NocodeCache {

        private final Map<Object, Object> store = new HashMap<>();

        @Override
        public Optional<Object> get(Object key) {
            return Optional.ofNullable(store.get(key));
        }

        @Override
        public void put(Object key, Object value) {
            store.put(key, value);
        }

        @Override
        public void evict(Object key) {
            store.remove(key);
        }
    }
}
