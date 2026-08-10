// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.contract;

public record PropertyContract(
        String name,
        String javaType,
        String description
) {
    public PropertyContract {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Property name must not be blank");
        }
        if (javaType == null || javaType.isBlank()) {
            throw new IllegalArgumentException("Property javaType must not be blank");
        }
        description = description == null ? "" : description;
    }
}
