package vip.isass.framework.nocode.v3.cache;

import java.util.Optional;

/**
 * Minimal cache facade for nocode v3 operation interceptors.
 */
public interface NocodeCache {

    Optional<Object> get(Object key);

    void put(Object key, Object value);

    void evict(Object key);

}
