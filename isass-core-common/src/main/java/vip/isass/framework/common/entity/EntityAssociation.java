// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.entity;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generated relationship metadata for nocode response loading.
 * This is deliberately metadata rather than an ORM annotation.
 */
public record EntityAssociation(
        String property,
        Kind kind,
        Class<? extends IEntity<?>> targetType,
        String localField,
        String targetField,
        boolean cascadeDelete,
        boolean writable,
        Map<String, String> fixedValues
) {

    public EntityAssociation {
        fixedValues = fixedValues == null ? Map.of() : Map.copyOf(fixedValues);
        if (!writable && cascadeDelete) {
            throw new IllegalArgumentException("只读关联不能级联删除");
        }
    }

    public EntityAssociation(String property, Kind kind, Class<? extends IEntity<?>> targetType,
                             String localField, String targetField, boolean cascadeDelete) {
        this(property, kind, targetType, localField, targetField, cascadeDelete, true, Map.of());
    }

    public EntityAssociation readOnly() {
        return new EntityAssociation(property, kind, targetType, localField, targetField,
                false, false, fixedValues);
    }

    public EntityAssociation where(String field, String value) {
        Map<String, String> conditions = new LinkedHashMap<>(fixedValues);
        conditions.put(field, value);
        return new EntityAssociation(property, kind, targetType, localField, targetField,
                cascadeDelete, writable, conditions);
    }

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
        return one(property, targetType, localField, targetField, false);
    }

    public static EntityAssociation one(String property, Class<? extends IEntity<?>> targetType,
                                        String localField, String targetField, boolean cascadeDelete) {
        return new EntityAssociation(property, Kind.ONE, targetType, localField, targetField, cascadeDelete);
    }

    public static EntityAssociation many(
            String property,
            Class<? extends IEntity<?>> targetType,
            String localField,
            String targetField
    ) {
        return many(property, targetType, localField, targetField, false);
    }

    public static EntityAssociation many(String property, Class<? extends IEntity<?>> targetType,
                                         String localField, String targetField, boolean cascadeDelete) {
        return new EntityAssociation(property, Kind.MANY, targetType, localField, targetField, cascadeDelete);
    }
}
