// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.lifecycle;

/** Receives lifecycle callbacks around the canonical NoCode query executor. */
public interface CrudQueryLifecycleListener {

    default boolean supports(CrudQueryLifecycleContext<?, ?, ?> context) {
        return true;
    }

    default void beforeQuery(CrudQueryLifecycleContext<?, ?, ?> context) {
    }

    default void afterQuery(CrudQueryLifecycleContext<?, ?, ?> context) {
    }

    default void onFailure(CrudQueryLifecycleContext<?, ?, ?> context, Throwable error) {
    }
}
