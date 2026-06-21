package vip.isass.framework.nocode.v3.access;

import java.util.Collections;
import java.util.List;

/**
 * Fetch options passed from access adapters to nocode v3 providers.
 */
public record NocodeFetchOptions(
        boolean includeRelations,
        List<String> relationNames
) {

    public NocodeFetchOptions {
        relationNames = relationNames == null ? List.of() : Collections.unmodifiableList(List.copyOf(relationNames));
    }

    public static NocodeFetchOptions none() {
        return new NocodeFetchOptions(false, List.of());
    }

    public static NocodeFetchOptions include(String... relationNames) {
        return new NocodeFetchOptions(true, names(relationNames));
    }

    public boolean hasRelationFilter() {
        return !relationNames.isEmpty();
    }

    private static List<String> names(String... relationNames) {
        if (relationNames == null || relationNames.length == 0) {
            return List.of();
        }
        return List.of(relationNames);
    }
}
