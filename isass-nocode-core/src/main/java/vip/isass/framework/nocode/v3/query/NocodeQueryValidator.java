package vip.isass.framework.nocode.v3.query;

import vip.isass.framework.nocode.v3.model.NocodeEntityDefinition;
import vip.isass.framework.nocode.v3.model.NocodeFieldDefinition;

import java.util.Objects;

/**
 * Validates query criteria against nocode v3 entity metadata.
 */
public final class NocodeQueryValidator {

    private NocodeQueryValidator() {
    }

    public static void validate(NocodeEntityDefinition definition, NocodeQueryCriteria criteria) {
        Objects.requireNonNull(definition, "definition");
        if (criteria == null) {
            return;
        }
        for (String fieldName : criteria.selectFields()) {
            requireKnown(definition, fieldName);
        }
        for (NocodeQueryCondition condition : criteria.conditions()) {
            validateCondition(definition, condition);
        }
        for (NocodeQueryGroup group : criteria.groups()) {
            for (NocodeQueryCondition condition : group.conditions()) {
                validateCondition(definition, condition);
            }
        }
        for (NocodeSort sort : criteria.sorts()) {
            NocodeFieldDefinition field = requireKnown(definition, sort.fieldName());
            if (!field.sortable()) {
                throw new NocodeQueryValidationException("Field is not sortable: " + sort.fieldName());
            }
        }
    }

    private static void validateCondition(NocodeEntityDefinition definition, NocodeQueryCondition condition) {
        NocodeFieldDefinition field = requireKnown(definition, condition.fieldName());
        if (!field.queryable()) {
            throw new NocodeQueryValidationException("Field is not queryable: " + condition.fieldName());
        }
    }

    private static NocodeFieldDefinition requireKnown(NocodeEntityDefinition definition, String fieldName) {
        return definition.field(fieldName)
                .orElseThrow(() -> new NocodeQueryValidationException(
                        "Unknown field: " + fieldName + " for entity: " + definition.entityName()));
    }
}
