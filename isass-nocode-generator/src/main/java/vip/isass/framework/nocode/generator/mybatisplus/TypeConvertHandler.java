// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.generator.mybatisplus;

import com.baomidou.mybatisplus.generator.config.GlobalConfig;
import com.baomidou.mybatisplus.generator.config.po.TableField;
import com.baomidou.mybatisplus.generator.config.rules.DbColumnType;
import com.baomidou.mybatisplus.generator.config.rules.IColumnType;
import com.baomidou.mybatisplus.generator.type.ITypeConvertHandler;
import com.baomidou.mybatisplus.generator.type.TypeRegistry;

public class TypeConvertHandler implements ITypeConvertHandler {
    @Override
    public IColumnType convert(GlobalConfig globalConfig, TypeRegistry typeRegistry, TableField.MetaInfo metaInfo) {
        String typeName = metaInfo.getTypeName().toLowerCase();

        // 数字类型
        if (typeName.equals("smallint[]")) {
            return ExtDbColumnType.SHORT_ARRAY;
        } else if (typeName.equals("tinyint")) {
            return DbColumnType.INTEGER;
        } else if (typeName.equals("tinyint[]") || typeName.equals("integer[]")) {
            return ExtDbColumnType.INTEGER_ARRAY;
        } else if (typeName.equals("bigint[]")) {
            return ExtDbColumnType.LONG_ARRAY;
        } else if (typeName.startsWith("numeric") && typeName.endsWith("[]")) {
            return ExtDbColumnType.BIG_DECIMAL_ARRAY;
        }

        // 布尔
        else if (typeName.equals("boolean[]")) {
            return ExtDbColumnType.BOOLEAN_ARRAY;
        }

        // 字符串
        else if (typeName.startsWith("character") && typeName.endsWith("[]")) {
            return ExtDbColumnType.STRING_COLLECTION;
        } else if (typeName.equals("text[]")) {
            return ExtDbColumnType.STRING_COLLECTION;
        }

        // 日期时间类型
        else if (typeName.equals("data[]")) {
            switch (globalConfig.getDateType()) {
                case ONLY_DATE:
                    return ExtDbColumnType.DATE_ARRAY;
                case SQL_PACK:
                    return ExtDbColumnType.DATE_SQL_ARRAY;
                case TIME_PACK:
                    return ExtDbColumnType.LOCAL_DATE_ARRAY;
            }
        } else if (typeName.startsWith("timestamp") && typeName.endsWith("[]")) {
            switch (globalConfig.getDateType()) {
                case ONLY_DATE:
                    return ExtDbColumnType.DATE_ARRAY;
                case SQL_PACK:
                    return ExtDbColumnType.TIMESTAMP_ARRAY;
                case TIME_PACK:
                    return ExtDbColumnType.LOCAL_DATE_TIME_ARRAY;
            }
        } else if (typeName.startsWith("time") && typeName.endsWith("[]")) {
            switch (globalConfig.getDateType()) {
                case ONLY_DATE:
                    return ExtDbColumnType.DATE_ARRAY;
                case SQL_PACK:
                    return ExtDbColumnType.TIME_ARRAY;
                case TIME_PACK:
                    return ExtDbColumnType.LOCAL_TIME_ARRAY;
            }
        }

        // json 类型
        else if (typeName.equals("json") || typeName.equals("jsonb")) {
            return ExtDbColumnType.JSON;
        } else if (typeName.equals("json[]") || typeName.equals("jsonb[]")) {
            return ExtDbColumnType.JSON_ARRAY;
        }
        return typeRegistry.getColumnType(metaInfo);
    }
}
