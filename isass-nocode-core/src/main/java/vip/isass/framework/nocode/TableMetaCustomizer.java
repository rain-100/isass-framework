// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode;

/**
 * Declares exceptional property-to-column mappings before MyBatis-Plus initializes table metadata.
 * Implementations must have a no-argument constructor and should be registered as Spring components.
 */
@FunctionalInterface
public interface TableMetaCustomizer {

    void customize(TableMetaRegistrar registrar);
}
