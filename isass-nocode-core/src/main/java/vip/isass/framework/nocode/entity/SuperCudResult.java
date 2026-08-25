// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.entity;

/** Aggregate affected-row counts for one {@link SuperCudReq}. */
public record SuperCudResult(
        long addedCount,
        long updatedCount,
        long deletedCount
) {

    public static SuperCudResult empty() {
        return new SuperCudResult(0, 0, 0);
    }
}
