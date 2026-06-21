package vip.isass.framework.nocode.v3.access;

import java.util.Collections;
import java.util.List;

/**
 * Delete options passed from access adapters to nocode v3 providers.
 */
public record NocodeDeleteOptions(
        boolean cascadeDelete,
        boolean associatedDelete,
        List<String> relationNames
) {

    public NocodeDeleteOptions {
        relationNames = relationNames == null ? List.of() : Collections.unmodifiableList(List.copyOf(relationNames));
    }

    public static NocodeDeleteOptions none() {
        return new NocodeDeleteOptions(false, false, List.of());
    }

    public static NocodeDeleteOptions cascade(String... relationNames) {
        return new NocodeDeleteOptions(true, false, names(relationNames));
    }

    public static NocodeDeleteOptions associated(String... relationNames) {
        return new NocodeDeleteOptions(false, true, names(relationNames));
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
