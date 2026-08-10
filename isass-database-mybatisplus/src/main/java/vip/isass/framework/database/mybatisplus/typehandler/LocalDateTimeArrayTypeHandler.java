// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.database.mybatisplus.typehandler;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;

import java.sql.*;
import java.time.LocalDateTime;

/**
 * 处理字段类型为 LocalDateTime[] 的数据库映射关系
 *
 * @author Rain
 */
@Slf4j
@MappedJdbcTypes(JdbcType.ARRAY)
public class LocalDateTimeArrayTypeHandler extends BaseTypeHandler<LocalDateTime[]> {

    private static final String TYPE_NAME_TIMESTAMP = "timestamptz";

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, LocalDateTime[] parameter, JdbcType jdbcType) throws SQLException {
        Connection conn = ps.getConnection();
        Array array = conn.createArrayOf(TYPE_NAME_TIMESTAMP, parameter);
        ps.setArray(i, array);
    }

    @Override
    public LocalDateTime[] getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return getArray(rs.getArray(columnName));
    }

    @Override
    public LocalDateTime[] getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return getArray(rs.getArray(columnIndex));
    }

    @Override
    public LocalDateTime[] getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return getArray(cs.getArray(columnIndex));
    }

    private LocalDateTime[] getArray(Array array) {
        if (array == null) {
            return null;
        }
        try {
            Timestamp[] timestamps = (Timestamp[]) array.getArray();
            LocalDateTime[] localDateTimes = new LocalDateTime[timestamps.length];
            for (int i = 0; i < timestamps.length; i++) {
                localDateTimes[i] = timestamps[i].toLocalDateTime();
            }
            return localDateTimes;
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }


}
