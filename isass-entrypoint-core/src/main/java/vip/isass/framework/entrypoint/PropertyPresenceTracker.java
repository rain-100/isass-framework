// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.entrypoint;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Identity-based weak storage for transient property-presence metadata. */
final class PropertyPresenceTracker {

    private static final ReferenceQueue<Object> QUEUE = new ReferenceQueue<>();
    private static final ConcurrentMap<IdentityReference, Set<String>> VALUES = new ConcurrentHashMap<>();

    private PropertyPresenceTracker() {
    }

    static void mark(Object target, String property) {
        if (property == null || property.isBlank()) {
            throw new IllegalArgumentException("property 不能为空");
        }
        expungeStaleEntries();
        VALUES.computeIfAbsent(new IdentityReference(target, QUEUE), ignored -> ConcurrentHashMap.newKeySet())
                .add(property);
    }

    static boolean contains(Object target, String property) {
        expungeStaleEntries();
        Set<String> properties = VALUES.get(new IdentityReference(target));
        return properties != null && properties.contains(property);
    }

    static Set<String> properties(Object target) {
        expungeStaleEntries();
        Set<String> properties = VALUES.get(new IdentityReference(target));
        return properties == null ? Set.of()
                : Collections.unmodifiableSet(new LinkedHashSet<>(properties));
    }

    private static void expungeStaleEntries() {
        IdentityReference reference;
        while ((reference = (IdentityReference) QUEUE.poll()) != null) {
            VALUES.remove(reference);
        }
    }

    private static final class IdentityReference extends WeakReference<Object> {

        private final int hash;

        private IdentityReference(Object referent) {
            super(referent);
            hash = System.identityHashCode(referent);
        }

        private IdentityReference(Object referent, ReferenceQueue<Object> queue) {
            super(referent, queue);
            hash = System.identityHashCode(referent);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            return other instanceof IdentityReference reference
                    && get() != null && get() == reference.get();
        }
    }
}
