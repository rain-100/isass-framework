package vip.isass.framework.nocode.v3.validation;

/**
 * One validation failure produced from nocode v3 metadata.
 */
public record NocodeValidationViolation(
        String entityName,
        String fieldName,
        NocodeFieldConstraintType constraintType,
        String message
) {
}
