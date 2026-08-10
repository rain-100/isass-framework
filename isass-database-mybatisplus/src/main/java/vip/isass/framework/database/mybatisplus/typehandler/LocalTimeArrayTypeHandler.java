// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.database.mybatisplus.typehandler;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;

import java.sql.*;
import java.time.LocalTime;

/**
 * 处理字段类型为 LocalTime[] 的数据库映射关系
 *
 * @author Rain
 */
@Slf4j
@MappedJdbcTypes(JdbcType.ARRAY)
public class LocalTimeArrayTypeHandler extends BaseTypeHandler<LocalTime[]> {

    private static final String TYPE_NAME_TIMESTAMP = "timetz";

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, LocalTime[] parameter, JdbcType jdbcType) throws SQLException {
        Connection conn = ps.getConnection();
        Array array = conn.createArrayOf(TYPE_NAME_TIMESTAMP, parameter);
        ps.setArray(i, array);
    }

    @Override
    public LocalTime[] getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return getArray(rs.getArray(columnName));
    }

    @Override
    public LocalTime[] getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return getArray(rs.getArray(columnIndex));
    }

    @Override
    public LocalTime[] getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return getArray(cs.getArray(columnIndex));
    }

    private LocalTime[] getArray(Array array) {
        if (array == null) {
            return null;
        }
        try {
            Time[] times = (Time[]) array.getArray();
            LocalTime[] localTimes = new LocalTime[times.length];
            for (int i = 0; i < times.length; i++) {
                localTimes[i] = times[i].toLocalTime();
            }
            return localTimes;
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }


}
