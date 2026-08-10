// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.database.mybatisplus.typehandler;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;

import java.sql.*;
import java.time.LocalDate;

/**
 * 处理字段类型为 LocalDate[] 的数据库映射关系
 *
 * @author Rain
 */
@Slf4j
@MappedJdbcTypes(JdbcType.ARRAY)
public class LocalDateArrayTypeHandler extends BaseTypeHandler<LocalDate[]> {

    private static final String TYPE_NAME_TIMESTAMP = "date";

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, LocalDate[] parameter, JdbcType jdbcType) throws SQLException {
        Connection conn = ps.getConnection();
        Array array = conn.createArrayOf(TYPE_NAME_TIMESTAMP, parameter);
        ps.setArray(i, array);
    }

    @Override
    public LocalDate[] getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return getArray(rs.getArray(columnName));
    }

    @Override
    public LocalDate[] getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return getArray(rs.getArray(columnIndex));
    }

    @Override
    public LocalDate[] getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return getArray(cs.getArray(columnIndex));
    }

    private LocalDate[] getArray(Array array) {
        if (array == null) {
            return null;
        }
        try {
            Date[] dates = (Date[]) array.getArray();
            LocalDate[] localDates = new LocalDate[dates.length];
            for (int i = 0; i < dates.length; i++) {
                localDates[i] = dates[i].toLocalDate();
            }
            return localDates;
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }


}
