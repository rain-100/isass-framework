package vip.isass.framework.nocode.v3.cache;

import vip.isass.framework.nocode.v3.operation.NocodeOperation;

/**
 * Generates cache keys for nocode v3 operations.
 */
@FunctionalInterface
public interface NocodeCacheKeyGenerator {

    Object generate(NocodeOperation operation);

}
