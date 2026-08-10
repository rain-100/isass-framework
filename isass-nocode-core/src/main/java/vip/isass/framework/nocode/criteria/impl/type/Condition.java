// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.criteria.impl.type;

import lombok.Getter;

/**
 * @author Rain
 */
@Getter
public enum Condition {

    // 条件之间的关系
    OR(""),

    // 通用类型
    EQUAL(""),
    NOT_EQUAL("NotEqual"),

    IN("In"),
    NOT_IN("NotIn"),

    IS_NULL("IsNull"),
    IS_NOT_NULL("IsNotNull"),

    IS_EMPTY("IsEmpty"),
    IS_NOT_EMPTY("IsNotEmpty"),

    // 数字类型
    GREATER_THAN("GreaterThan"),
    GREATER_THAN_EQUAL("GreaterThanEqual"),
    LESS_THAN("LessThan"),
    LESS_THAN_EQUAL("LessThanEqual"),

    // 字符串类型
    START_WITH("StartWith"),
    LIKE("Like"),
    NOT_LIKE("NotLike"),

    // 数组类型
    CONTAINS_ALL("ContainsAll"),
    CONTAINS_ANY("ContainsAny"),

    // json 对象
    JSON_OBJECT_PATH_EQUAL("JsonObjectPathEqual"),
    JSON_OBJECT_PATH_LIKE("JsonObjectPathLike"),

    // json 数组
    JSON_ARRAY_CONTAINS("JsonArrayContains"),
    JSON_ARRAY_CONTAINS_ANY("JsonArrayContainsAny"),
    JSON_ARRAY_CONTAINS_ALL("JsonArrayContainsAll"),
    ;

    /**
     * 属性名后缀
     */
    private final String propertyNameSuffix;

    Condition(String propertyNameSuffix) {
        this.propertyNameSuffix = propertyNameSuffix;
    }

}
