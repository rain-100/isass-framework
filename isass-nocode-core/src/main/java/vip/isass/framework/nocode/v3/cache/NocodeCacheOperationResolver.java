package vip.isass.framework.nocode.v3.cache;

import vip.isass.framework.nocode.v3.operation.NocodeOperation;

import java.util.Optional;

/**
 * Resolves cache metadata for a nocode v3 operation.
 */
@FunctionalInterface
public interface NocodeCacheOperationResolver {

    Optional<NocodeCacheOperation> resolve(NocodeOperation operation);

}
