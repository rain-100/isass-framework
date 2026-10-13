// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.generator.association;

import java.util.Map;

/** Structured, directional association metadata consumed by templates. */
public record GeneratorAssociation(
        String property,
        String targetEntity,
        Kind kind,
        String localKey,
        String targetKey,
        boolean cascadeDelete,
        boolean writable,
        Map<String, String> fixedValues
) {
    public GeneratorAssociation {
        fixedValues = fixedValues == null ? Map.of() : Map.copyOf(fixedValues);
    }

    public GeneratorAssociation(String property, String targetEntity, Kind kind,
                                String localKey, String targetKey, boolean cascadeDelete) {
        this(property, targetEntity, kind, localKey, targetKey, cascadeDelete, true, Map.of());
    }

    public String modifiers() {
        StringBuilder result = new StringBuilder(writable ? "" : ".readOnly()");
        fixedValues.forEach((field, value) -> result.append(".where(\"")
                .append(escape(field)).append("\", \"").append(escape(value)).append("\")"));
        return result.toString();
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    public enum Kind { ONE, MANY }
}
