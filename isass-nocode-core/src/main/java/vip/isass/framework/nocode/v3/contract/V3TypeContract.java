package vip.isass.framework.nocode.v3.contract;

import java.util.List;

public record V3TypeContract(
        String javaType,
        String schemaName,
        String description,
        List<V3PropertyContract> properties
) {
    public V3TypeContract {
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
