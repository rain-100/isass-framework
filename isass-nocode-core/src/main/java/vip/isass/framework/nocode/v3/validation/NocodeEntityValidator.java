package vip.isass.framework.nocode.v3.validation;

import vip.isass.framework.nocode.v3.model.NocodeEntityDefinition;
import vip.isass.framework.nocode.v3.model.NocodeFieldDefinition;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Validates map-style request bodies with nocode v3 field metadata.
 */
public class NocodeEntityValidator {

    public List<NocodeValidationViolation> validate(
            NocodeEntityDefinition entityDefinition,
            Map<String, ?> body,
            NocodeValidationGroup group
    ) {
        Objects.requireNonNull(entityDefinition, "entityDefinition");
        Objects.requireNonNull(group, "group");
        List<NocodeValidationViolation> violations = new ArrayList<>();
        Map<String, ?> values = body == null ? Map.of() : body;

        for (NocodeFieldDefinition field : entityDefinition.fields()) {
            Object value = values.get(field.fieldName());
            for (NocodeFieldConstraint constraint : field.constraints()) {
                if (constraint.appliesTo(group) && !isValid(constraint, value)) {
                    violations.add(new NocodeValidationViolation(
                            entityDefinition.entityName(),
                            field.fieldName(),
                            constraint.type(),
                            constraint.message()
                    ));
                }
            }
        }
        return List.copyOf(violations);
    }

    public void validateAndThrow(
            NocodeEntityDefinition entityDefinition,
            Map<String, ?> body,
            NocodeValidationGroup group
    ) {
        List<NocodeValidationViolation> violations = validate(entityDefinition, body, group);
        if (!violations.isEmpty()) {
            throw new NocodeEntityValidationException(violations);
        }
    }

    private boolean isValid(NocodeFieldConstraint constraint, Object value) {
        return switch (constraint.type()) {
            case NOT_NULL -> value != null;
            case NOT_BLANK -> value instanceof CharSequence text && !text.toString().isBlank();
            case SIZE -> value == null || isSizeValid(value, constraint.min(), constraint.max());
        };
    }

    private boolean isSizeValid(Object value, Integer min, Integer max) {
        Integer size = sizeOf(value);
        if (size == null) {
            return false;
        }
        return (min == null || size >= min) && (max == null || size <= max);
    }

    private Integer sizeOf(Object value) {
        if (value instanceof CharSequence text) {
            return text.length();
        }
        if (value instanceof Collection<?> collection) {
            return collection.size();
        }
        if (value instanceof Map<?, ?> map) {
            return map.size();
        }
        if (value.getClass().isArray()) {
            return Array.getLength(value);
        }
        return null;
    }
}
