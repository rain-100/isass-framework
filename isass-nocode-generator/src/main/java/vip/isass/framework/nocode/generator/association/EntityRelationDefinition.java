// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.generator.association;

import java.util.Objects;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.lang.model.SourceVersion;

/**
 * Java 关系声明，不依赖尚未生成的实体类。生成器据此生成类型化成员及 EntityAssociation。
 */
public record EntityRelationDefinition(
        String sourceEntity,
        String property,
        String targetEntity,
        GeneratorAssociation.Kind kind,
        String localKey,
        String targetKey,
        boolean cascadeDelete,
        boolean writable,
        Map<String, String> fixedValues
) {
    public EntityRelationDefinition {
        identifier(sourceEntity, "源实体");
        identifier(targetEntity, "目标实体");
        identifier(property, "关系属性");
        identifier(localKey, "源关联键");
        identifier(targetKey, "目标关联键");
        Objects.requireNonNull(kind, "关系类型");
        fixedValues = fixedValues == null ? Map.of() : Map.copyOf(fixedValues);
        fixedValues.keySet().forEach(field -> identifier(field, "固定条件字段"));
        if (!writable && cascadeDelete) {
            throw new IllegalArgumentException("只读关联不能级联删除");
        }
    }

    public EntityRelationDefinition(String sourceEntity, String property, String targetEntity,
                                    GeneratorAssociation.Kind kind, String localKey,
                                    String targetKey, boolean cascadeDelete) {
        this(sourceEntity, property, targetEntity, kind, localKey, targetKey, cascadeDelete,
                true, Map.of());
    }

    public EntityRelationDefinition readOnly() {
        return new EntityRelationDefinition(sourceEntity, property, targetEntity, kind,
                localKey, targetKey, false, false, fixedValues);
    }

    public EntityRelationDefinition where(String field, String value) {
        Map<String, String> conditions = new LinkedHashMap<>(fixedValues);
        conditions.put(field, value);
        return new EntityRelationDefinition(sourceEntity, property, targetEntity, kind,
                localKey, targetKey, cascadeDelete, writable, conditions);
    }

    public static EntityRelationDefinition one(
            String sourceEntity, String property, String targetEntity, String localKey, String targetKey) {
        return one(sourceEntity, property, targetEntity, localKey, targetKey, false);
    }

    public static EntityRelationDefinition one(
            String sourceEntity, String property, String targetEntity,
            String localKey, String targetKey, boolean cascadeDelete) {
        return new EntityRelationDefinition(sourceEntity, property, targetEntity,
                GeneratorAssociation.Kind.ONE, localKey, targetKey, cascadeDelete);
    }

    public static EntityRelationDefinition many(
            String sourceEntity, String property, String targetEntity, String localKey, String targetKey) {
        return many(sourceEntity, property, targetEntity, localKey, targetKey, false);
    }

    public static EntityRelationDefinition many(
            String sourceEntity, String property, String targetEntity,
            String localKey, String targetKey, boolean cascadeDelete) {
        return new EntityRelationDefinition(sourceEntity, property, targetEntity,
                GeneratorAssociation.Kind.MANY, localKey, targetKey, cascadeDelete);
    }

    public GeneratorAssociation association() {
        return new GeneratorAssociation(property, targetEntity, kind, localKey, targetKey,
                cascadeDelete, writable, fixedValues);
    }

    private static void identifier(String value, String label) {
        if (value == null || !SourceVersion.isIdentifier(value) || SourceVersion.isKeyword(value)) {
            throw new IllegalArgumentException(label + " 必须是合法的 Java 标识符: " + value);
        }
    }
}
