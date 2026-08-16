// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.entity;

/** One conditional-create item in a {@link SuperCudReq}. */
public record AddIfAbsentItem<E, C>(E entity, C criteria) {
}
