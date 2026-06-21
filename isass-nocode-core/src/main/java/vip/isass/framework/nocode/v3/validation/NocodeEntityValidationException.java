package vip.isass.framework.nocode.v3.validation;

import java.util.List;

public class NocodeEntityValidationException extends IllegalArgumentException {

    private final List<NocodeValidationViolation> violations;

    public NocodeEntityValidationException(List<NocodeValidationViolation> violations) {
        super(formatMessage(violations));
        this.violations = List.copyOf(violations);
    }

    public List<NocodeValidationViolation> violations() {
        return violations;
    }

    private static String formatMessage(List<NocodeValidationViolation> violations) {
        if (violations == null || violations.isEmpty()) {
            return "Validation failed";
        }
        NocodeValidationViolation first = violations.getFirst();
        return first.fieldName() + ": " + first.message();
    }
}
