// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.lifecycle;

/** Receives lifecycle callbacks around the canonical NoCode {@code superCud} execution. */
public interface CrudWriteLifecycleListener {

    default boolean supports(CrudWriteLifecycleContext<?, ?> context) {
        return true;
    }

    /** Runs inside the write transaction before repository changes. */
    default void beforeExecute(CrudWriteLifecycleContext<?, ?> context) {
    }

    /** Runs inside the write transaction after repository changes and before commit. */
    default void afterExecute(CrudWriteLifecycleContext<?, ?> context) {
    }

    /** Runs only after the surrounding transaction has committed successfully. */
    default void afterCommit(CrudWriteLifecycleContext<?, ?> context) {
    }

    /** Runs after rollback; {@code error} may be {@code null} for rollback-only transactions. */
    default void afterRollback(CrudWriteLifecycleContext<?, ?> context, Throwable error) {
    }
}
