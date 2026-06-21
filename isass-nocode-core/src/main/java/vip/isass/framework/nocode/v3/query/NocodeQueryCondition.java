package vip.isass.framework.nocode.v3.query;

import java.util.Objects;

public record NocodeQueryCondition(
        String fieldName,
        NocodeQueryOperator operator,
        Object value
) {

    public NocodeQueryCondition {
        fieldName = requireText(fieldName, "fieldName");
        operator = operator == null ? NocodeQueryOperator.EQUALS : operator;
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
