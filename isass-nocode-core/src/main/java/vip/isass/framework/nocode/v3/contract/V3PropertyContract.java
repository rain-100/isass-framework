package vip.isass.framework.nocode.v3.contract;

public record V3PropertyContract(
        String name,
        String javaType,
        String description
) {
    public V3PropertyContract {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Property name must not be blank");
        }
        if (javaType == null || javaType.isBlank()) {
            throw new IllegalArgumentException("Property javaType must not be blank");
        }
        description = description == null ? "" : description;
    }
}
