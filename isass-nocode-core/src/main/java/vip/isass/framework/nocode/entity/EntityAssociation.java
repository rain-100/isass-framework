// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.entity;

/**
 * Generated relationship metadata for nocode response loading.
 * This is deliberately metadata rather than an ORM annotation.
 */
public record EntityAssociation(
        String property,
        Kind kind,
        Class<? extends IEntity<?>> targetType,
        String localField,
        String targetField
) {

    public enum Kind {
        ONE,
        MANY
    }

    public static EntityAssociation one(
            String property,
            Class<? extends IEntity<?>> targetType,
            String localField,
            String targetField
    ) {
        return new EntityAssociation(property, Kind.ONE, targetType, localField, targetField);
    }

    public static EntityAssociation many(
            String property,
            Class<? extends IEntity<?>> targetType,
            String localField,
            String targetField
    ) {
        return new EntityAssociation(property, Kind.MANY, targetType, localField, targetField);
    }
}
