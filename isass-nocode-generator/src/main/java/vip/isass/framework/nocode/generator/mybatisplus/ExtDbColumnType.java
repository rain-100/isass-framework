// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.generator.mybatisplus;

import com.baomidou.mybatisplus.generator.config.rules.IColumnType;
import lombok.Getter;

/**
 * @author Rain
 */
public enum ExtDbColumnType implements IColumnType {

    SHORT_ARRAY("Short[]", null),
    INTEGER_ARRAY("Integer[]", null),
    LONG_ARRAY("Long[]", null),
    BIG_DECIMAL_ARRAY("BigDecimal[]", null),
    BOOLEAN_ARRAY("Boolean[]", null),
    STRING_ARRAY("String[]", null),
    STRING_COLLECTION("Collection<String>", null),
    DATE_ARRAY("Date[]", null),
    DATE_SQL_ARRAY("Date[]", "java.sql.Date"),
    LOCAL_DATE_ARRAY("LocalDate[]", "java.time.LocalDate"),
    TIMESTAMP_ARRAY("Timestamp[]", "java.sql.Timestamp"),
    LOCAL_DATE_TIME_ARRAY("LocalDateTime[]", "java.time.LocalDateTime"),
    TIME_ARRAY("Time[]", "java.sql.Time"),
    LOCAL_TIME_ARRAY("LocalTime[]", "java.time.LocalTime"),
    JSON("JsonNode", "tools.jackson.databind.JsonNode"),
    JSON_ARRAY("JsonNode[]", "tools.jackson.databind.JsonNode");

    /**
     * 类型
     */
    @Getter
    private final String type;

    /**
     * 包路径
     */
    @Getter
    private final String pkg;

    ExtDbColumnType(final String type, final String pkg) {
        this.type = type;
        this.pkg = pkg;
    }

}
