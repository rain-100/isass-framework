// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.entrypoint;

import java.beans.Introspector;
import java.util.Set;

/**
 * Carries the transient set of properties explicitly supplied for one invocation.
 * The set is deliberately transport-neutral and is never part of the business payload.
 */
public interface PropertyPresenceAware {

    default void markPresentProperty(String property) {
        PropertyPresenceTracker.mark(this, property);
    }

    default boolean isPropertyPresent(String property) {
        return PropertyPresenceTracker.contains(this, property);
    }

    default Set<String> presentProperties() {
        return PropertyPresenceTracker.properties(this);
    }

    /** Explicit local-call escape hatch for operations such as update-all-columns. */
    default void markAllPresentProperties() {
        try {
            for (var descriptor : Introspector.getBeanInfo(getClass()).getPropertyDescriptors()) {
                if (!"class".equals(descriptor.getName()) && descriptor.getWriteMethod() != null) {
                    markPresentProperty(descriptor.getName());
                }
            }
        } catch (java.beans.IntrospectionException exception) {
            throw new IllegalStateException("无法分析属性存在性: " + getClass().getName(), exception);
        }
    }
}
