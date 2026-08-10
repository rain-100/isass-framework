// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.contract;

import java.util.List;

public record TypeContract(
        String javaType,
        String schemaName,
        String description,
        List<PropertyContract> properties
) {
    public TypeContract {
        if (javaType == null || javaType.isBlank()) {
            throw new IllegalArgumentException("Type javaType must not be blank");
        }
        if (schemaName == null || schemaName.isBlank()) {
            throw new IllegalArgumentException("Type schemaName must not be blank");
        }
        description = description == null ? "" : description;
        properties = List.copyOf(properties == null ? List.of() : properties);
    }
}
