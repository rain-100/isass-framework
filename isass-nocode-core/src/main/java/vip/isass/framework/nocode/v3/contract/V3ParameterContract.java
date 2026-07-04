package vip.isass.framework.nocode.v3.contract;

import java.util.Objects;

public record V3ParameterContract(
        String name,
        String javaType,
        V3ParameterSource source,
        boolean required,
        String description
) {
    public V3ParameterContract {
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
