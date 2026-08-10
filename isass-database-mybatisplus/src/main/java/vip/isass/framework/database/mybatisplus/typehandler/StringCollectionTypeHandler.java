// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.database.mybatisplus.typehandler;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ArrayUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

/**
 * 处理字段类型为 {@code Collection<java.lang.String>} 的数据库映射关系
 *
 * @author Rain
 */
@Slf4j
@MappedTypes({Collection.class, ArrayList.class, HashSet.class})
@MappedJdbcTypes(JdbcType.ARRAY)
public class StringCollectionTypeHandler extends BaseTypeHandler<Collection<String>> {

    private static final String TYPE_NAME_VARCHAR = "varchar";

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Collection<String> parameter, JdbcType jdbcType) throws SQLException {
        Connection conn = ps.getConnection();
        Array array = conn.createArrayOf(TYPE_NAME_VARCHAR, ArrayUtil.toArray(parameter, String.class));
        ps.setArray(i, array);
    }

    @Override
    public Collection<String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return getList(rs.getArray(columnName));
    }

    @Override
    public Collection<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return getList(rs.getArray(columnIndex));
    }

    @Override
    public Collection<String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return getList(cs.getArray(columnIndex));
    }

    private Collection<String> getList(Array array) {
        if (array == null) {
            return null;
        }
        try {
            String[] strArr = (String[]) array.getArray();
            return CollUtil.toList(strArr);
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }


}
