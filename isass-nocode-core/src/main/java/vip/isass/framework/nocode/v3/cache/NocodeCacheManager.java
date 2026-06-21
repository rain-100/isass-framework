package vip.isass.framework.nocode.v3.cache;

import java.util.Optional;

/**
 * Framework-level cache manager facade.
 */
public interface NocodeCacheManager {

    Optional<NocodeCache> getCache(String cacheName);

}
