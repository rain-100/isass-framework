// SPDX-License-Identifier: LGPL-3.0-only
package vip.isass.framework.database.mybatisplus.orm;

import com.github.yulichang.extension.apt.matedata.BaseColumn;
import com.github.yulichang.extension.apt.matedata.Column;
import com.github.yulichang.toolkit.support.ColumnCache;

import java.util.Objects;

/**
 * 为每个 SQL 表实例创建独立的 MPJ 字段根对象。
 */
final class BaseColumnFactory {

    private BaseColumnFactory() {
    }

    static <T> BaseColumn<T> create(Class<T> type) {
        return new DefaultBaseColumn<>(Objects.requireNonNull(type, "type"));
    }

    static Column column(BaseColumn<?> table, String property) {
        // Criteria 只暴露 Java 属性；在创建 MPJ Column 时确认它是直接持久化属性。
        EntityPropertyColumnResolver.resolve(table.getColumnClass(), property);
        if (!ColumnCache.getMapField(table.getColumnClass()).containsKey(property)) {
            throw new IllegalArgumentException("关联字段必须是实体的直接持久化属性: " + property);
        }
        return new Column(table, property);
    }

    private static final class DefaultBaseColumn<T> extends BaseColumn<T> {
        private final Class<T> type;

        private DefaultBaseColumn(Class<T> type) {
            this.type = type;
        }

        @Override
        public Class<T> getColumnClass() {
            return type;
        }
    }
}
