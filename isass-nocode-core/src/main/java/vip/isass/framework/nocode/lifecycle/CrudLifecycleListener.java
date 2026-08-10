// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.lifecycle;

/** Receives lifecycle events around a standard nocode write operation. */
public interface CrudLifecycleListener {

    default boolean supports(CrudLifecycleContext context) {
        return true;
    }

    default void before(CrudLifecycleContext context) {
    }

    default void afterSuccess(CrudLifecycleContext context) {
    }

    default void onFailure(CrudLifecycleContext context, Throwable error) {
    }
}
