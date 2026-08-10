// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.contract;

import java.util.Objects;

public record ParameterContract(
        String name,
        String javaType,
        ParameterSource source,
        boolean required,
        String description
) {
    public ParameterContract {
        name = requireText(name, "name");
        javaType = requireText(javaType, "javaType");
        source = Objects.requireNonNull(source, "source");
        description = description == null ? "" : description;
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
