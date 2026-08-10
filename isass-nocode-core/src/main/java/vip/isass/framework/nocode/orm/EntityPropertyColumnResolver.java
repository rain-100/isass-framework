// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.orm;

import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import vip.isass.framework.nocode.TableMeta;
import vip.isass.framework.nocode.TableMetaRegistrar;

/**
 * 在 ORM 边界将 Java 属性名解析为数据库字段名。
 */
final class EntityPropertyColumnResolver {

    private EntityPropertyColumnResolver() {
    }

    static String resolve(Class<?> entityClass, String propertyPath) {
        if (propertyPath == null || propertyPath.isBlank()) {
            throw new IllegalArgumentException("属性名不能为空");
        }
        String[] path = propertyPath.split("\\.", 2);
        String columnName = resolveProperty(entityClass, path[0]);
        return path.length == 1 ? columnName : columnName + "." + path[1];
    }

    private static String resolveProperty(Class<?> entityClass, String propertyName) {
        TableMeta tableMeta = TableMetaRegistrar.get(entityClass);
        if (tableMeta != null && tableMeta.columnMappings().containsKey(propertyName)) {
            return tableMeta.columnMappings().get(propertyName);
        }

        TableInfo tableInfo = TableInfoHelper.getTableInfo(entityClass);
        if (tableInfo == null) {
            throw new IllegalStateException("实体未初始化 MyBatis-Plus 元数据：" + entityClass.getName());
        }
        if (propertyName.equals(tableInfo.getKeyProperty())) {
            return tableInfo.getKeyColumn();
        }
        for (TableFieldInfo fieldInfo : tableInfo.getFieldList()) {
            if (propertyName.equals(fieldInfo.getProperty())) {
                return fieldInfo.getColumn();
            }
        }
        throw new IllegalArgumentException("未知实体属性：" + entityClass.getSimpleName() + "." + propertyName);
    }
}
