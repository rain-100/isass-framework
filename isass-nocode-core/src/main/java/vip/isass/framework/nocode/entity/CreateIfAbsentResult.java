// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.entity;

/** Result of a conditional create. */
public record CreateIfAbsentResult<E>(boolean created, E entity) {
}
