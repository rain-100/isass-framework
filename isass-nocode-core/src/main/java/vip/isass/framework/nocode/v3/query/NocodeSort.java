package vip.isass.framework.nocode.v3.query;

import java.util.Objects;

public record NocodeSort(
        String fieldName,
        Direction direction
) {

    public NocodeSort {
        fieldName = requireText(fieldName, "fieldName");
        direction = direction == null ? Direction.ASC : direction;
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    public enum Direction {
        ASC,
        DESC
    }
}
