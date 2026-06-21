package vip.isass.framework.nocode.v3.model;

import java.util.Objects;

/**
 * Framework-neutral relationship metadata used by cascade and associated operations.
 */
public record NocodeEntityRelation(
        String relationName,
        String sourceEntityName,
        String targetEntityName,
        String sourceFieldName,
        String targetFieldName,
        NocodeEntityRelationType relationType
) {

    public NocodeEntityRelation {
        relationName = requireText(relationName, "relationName");
        sourceEntityName = requireText(sourceEntityName, "sourceEntityName");
        targetEntityName = requireText(targetEntityName, "targetEntityName");
        sourceFieldName = requireText(sourceFieldName, "sourceFieldName");
        targetFieldName = requireText(targetFieldName, "targetFieldName");
        relationType = relationType == null ? NocodeEntityRelationType.ONE_TO_MANY : relationType;
    }

    public static NocodeEntityRelation oneToMany(
            String relationName,
            String sourceEntityName,
            String targetEntityName,
            String sourceFieldName,
            String targetFieldName
    ) {
        return new NocodeEntityRelation(
                relationName,
                sourceEntityName,
                targetEntityName,
                sourceFieldName,
                targetFieldName,
                NocodeEntityRelationType.ONE_TO_MANY
        );
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
