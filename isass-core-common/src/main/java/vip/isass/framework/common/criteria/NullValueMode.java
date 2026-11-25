// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.criteria;

/**
 * Null handling strategy for scalar business properties.
 */
public enum NullValueMode {
    /** Ignore null values. */
    IGNORE_NULL,
    /** Write null for every writable business property without a value (full replacement). */
    WRITE_NULL,
    /** Write null only for explicitly submitted properties; omitted properties remain unchanged. */
    WRITE_PRESENT_NULL
}
