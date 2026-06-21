package vip.isass.framework.nocode.v3.validation;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Field-level validation metadata for nocode v3.
 */
public record NocodeFieldConstraint(
        NocodeFieldConstraintType type,
        Set<NocodeValidationGroup> groups,
        Integer min,
        Integer max,
        String message
) {

    public NocodeFieldConstraint {
        type = Objects.requireNonNull(type, "type");
        groups = groups == null || groups.isEmpty()
                ? Collections.emptySet()
                : Collections.unmodifiableSet(EnumSet.copyOf(groups));
        if (min != null && min < 0) {
            throw new IllegalArgumentException("min must not be negative");
        }
        if (max != null && max < 0) {
            throw new IllegalArgumentException("max must not be negative");
        }
        if (min != null && max != null && min > max) {
            throw new IllegalArgumentException("min must not be greater than max");
        }
        message = message == null || message.isBlank() ? defaultMessage(type, min, max) : message;
    }

    public static NocodeFieldConstraint notNull(NocodeValidationGroup... groups) {
        return new NocodeFieldConstraint(NocodeFieldConstraintType.NOT_NULL, groupSet(groups), null, null, null);
    }

    public static NocodeFieldConstraint notBlank(NocodeValidationGroup... groups) {
        return new NocodeFieldConstraint(NocodeFieldConstraintType.NOT_BLANK, groupSet(groups), null, null, null);
    }

    public static NocodeFieldConstraint size(int min, int max, NocodeValidationGroup... groups) {
        return new NocodeFieldConstraint(NocodeFieldConstraintType.SIZE, groupSet(groups), min, max, null);
    }

    public boolean appliesTo(NocodeValidationGroup group) {
        return groups.isEmpty() || groups.contains(group);
    }

    private static Set<NocodeValidationGroup> groupSet(NocodeValidationGroup... groups) {
        if (groups == null || groups.length == 0) {
            return Collections.emptySet();
        }
        EnumSet<NocodeValidationGroup> result = EnumSet.noneOf(NocodeValidationGroup.class);
        Collections.addAll(result, groups);
        return result;
    }

    private static String defaultMessage(NocodeFieldConstraintType type, Integer min, Integer max) {
        return switch (type) {
            case NOT_NULL -> "must not be null";
            case NOT_BLANK -> "must not be blank";
            case SIZE -> "size must be between " + min + " and " + max;
        };
    }
}
