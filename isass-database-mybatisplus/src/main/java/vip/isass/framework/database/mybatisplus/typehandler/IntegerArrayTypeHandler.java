// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.database.mybatisplus.typehandler;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;

import java.sql.*;

/**
 * 处理字段类型为 Integer[] 的数据库映射关系
 *
 * @author Rain
 */
@Slf4j
@MappedJdbcTypes(JdbcType.ARRAY)
public class IntegerArrayTypeHandler extends BaseTypeHandler<Integer[]> {

    private static final String TYPE_NAME_INTEGER = "integer";

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Integer[] parameter, JdbcType jdbcType) throws SQLException {
        Connection conn = ps.getConnection();
        Array array = conn.createArrayOf(TYPE_NAME_INTEGER, parameter);
        ps.setArray(i, array);
    }

    @Override
    public Integer[] getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return getArray(rs.getArray(columnName));
    }

    @Override
    public Integer[] getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return getArray(rs.getArray(columnIndex));
    }

    @Override
    public Integer[] getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return getArray(cs.getArray(columnIndex));
    }

    private Integer[] getArray(Array array) {
        if (array == null) {
            return null;
        }
        try {
            return (Integer[]) array.getArray();
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }


}
