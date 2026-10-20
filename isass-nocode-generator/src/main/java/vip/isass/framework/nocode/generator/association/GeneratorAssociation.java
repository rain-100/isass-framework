// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.generator.association;

import java.util.Map;
import java.util.Objects;

/** Structured, directional association metadata consumed by templates. */
public record GeneratorAssociation(
        String property,
        String targetEntity,
        Kind kind,
        String localKey,
        String targetKey,
        boolean cascadeDelete,
        Map<String, String> fixedValues,
        CollectionType collectionType
) {
    public GeneratorAssociation {
        validateCollectionType(kind, collectionType);
        fixedValues = fixedValues == null ? Map.of() : Map.copyOf(fixedValues);
    }

    public GeneratorAssociation(String property, String targetEntity, Kind kind,
                                String localKey, String targetKey, boolean cascadeDelete,
                                Map<String, String> fixedValues) {
        this(property, targetEntity, kind, localKey, targetKey, cascadeDelete, fixedValues,
                defaultCollectionType(kind));
    }

    public GeneratorAssociation(String property, String targetEntity, Kind kind,
                                String localKey, String targetKey, boolean cascadeDelete) {
        this(property, targetEntity, kind, localKey, targetKey, cascadeDelete, Map.of());
    }

    /** 字段及 setter 共用同一 Java 类型，避免集合声明在生成过程中丢失。 */
    public String javaType() {
        return kind == Kind.ONE ? targetEntity : collectionType.javaType + "<" + targetEntity + ">";
    }

    public String modifiers() {
        StringBuilder result = new StringBuilder();
        fixedValues.forEach((field, value) -> result.append(".where(\"")
                .append(escape(field)).append("\", \"").append(escape(value)).append("\")"));
        return result.toString();
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    static CollectionType defaultCollectionType(Kind kind) {
        Objects.requireNonNull(kind, "关系类型");
        return kind == Kind.MANY ? CollectionType.COLLECTION : null;
    }

    static void validateCollectionType(Kind kind, CollectionType collectionType) {
        Objects.requireNonNull(kind, "关系类型");
        if (kind == Kind.ONE && collectionType != null) {
            throw new IllegalArgumentException("单个关联不能声明集合类型");
        }
        if (kind == Kind.MANY && collectionType == null) {
            throw new IllegalArgumentException("多项关联必须声明 Collection 或 List");
        }
    }

    public enum CollectionType {
        COLLECTION("Collection"), LIST("List");

        private final String javaType;

        CollectionType(String javaType) {
            this.javaType = javaType;
        }
    }

    public enum Kind { ONE, MANY }
}
