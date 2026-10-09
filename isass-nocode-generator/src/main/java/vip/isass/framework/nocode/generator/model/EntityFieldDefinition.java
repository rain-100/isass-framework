// SPDX-License-Identifier: LGPL-3.0-only
package vip.isass.framework.nocode.generator.model;

import java.util.List;

/**
 * Java 属性的生成语义；列名仍由数据库元数据解析。
 */
public record EntityFieldDefinition(String propertyName, String javaType, List<EnumValue> enumValues) {
    public EntityFieldDefinition {
        if (propertyName == null || !propertyName.matches("[a-zA-Z_$][a-zA-Z0-9_$]*")) {
            throw new IllegalArgumentException("非法属性名: " + propertyName);
        }
        enumValues = List.copyOf(enumValues);
        if (javaType != null && !javaType.matches("[\\w<>?, .]+")) {
            throw new IllegalArgumentException("非法 Java 类型: " + javaType);
        }
        if (javaType != null && !enumValues.isEmpty()) {
            throw new IllegalArgumentException("字段不能同时声明类型和枚举: " + propertyName);
        }
        if (enumValues.stream().map(EnumValue::code).distinct().count() != enumValues.size()
                || enumValues.stream().map(EnumValue::name).distinct().count() != enumValues.size()) {
            throw new IllegalArgumentException("枚举值重复: " + propertyName);
        }
    }

    public static EntityFieldDefinition type(String property, String javaType) {
        return new EntityFieldDefinition(property, javaType, List.of());
    }

    public static EntityFieldDefinition enumeration(String property, EnumValue... values) {
        return new EntityFieldDefinition(property, null, List.of(values));
    }

    /**
     * 数字编码枚举常量及中文说明。
     */
    public record EnumValue(int code, String name, String description) {
        public EnumValue {
            if (name == null || !name.matches("[A-Z][A-Z0-9_]*") || description == null) {
                throw new IllegalArgumentException("非法枚举声明: " + name);
            }
        }
    }
}
