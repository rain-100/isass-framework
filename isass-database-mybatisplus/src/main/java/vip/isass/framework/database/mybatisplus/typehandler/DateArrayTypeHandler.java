// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.database.mybatisplus.typehandler;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;

import java.sql.*;
import java.util.Date;

/**
 * 处理字段类型为 Date[] 的数据库映射关系
 *
 * @author Rain
 */
@Slf4j
@MappedJdbcTypes(JdbcType.ARRAY)
public class DateArrayTypeHandler extends BaseTypeHandler<Date[]> {

    private static final String TYPE_NAME_BIGINT = "date";

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Date[] parameter, JdbcType jdbcType) throws SQLException {
        Connection conn = ps.getConnection();
        Array array = conn.createArrayOf(TYPE_NAME_BIGINT, parameter);
        ps.setArray(i, array);
    }

    @Override
    public Date[] getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return getArray(rs.getArray(columnName));
    }

    @Override
    public Date[] getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return getArray(rs.getArray(columnIndex));
    }

    @Override
    public Date[] getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return getArray(cs.getArray(columnIndex));
    }

    private Date[] getArray(Array array) {
        if (array == null) {
            return null;
        }
        try {
            return (Date[]) array.getArray();
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }


}
