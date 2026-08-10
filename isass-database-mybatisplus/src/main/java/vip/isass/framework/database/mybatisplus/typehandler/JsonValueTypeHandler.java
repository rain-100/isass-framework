// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.database.mybatisplus.typehandler;

import cn.hutool.core.util.StrUtil;
import lombok.SneakyThrows;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import vip.isass.framework.common.support.JsonUtil;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/** Maps business-level JSON maps and lists without exposing a Jackson tree in models. */
@MappedJdbcTypes(JdbcType.JAVA_OBJECT)
@MappedTypes({Map.class, List.class})
public class JsonValueTypeHandler extends BaseTypeHandler<Object> {

    @Override
    @SneakyThrows
    public void setNonNullParameter(PreparedStatement statement, int index, Object value, JdbcType jdbcType) {
        statement.setString(index, JsonUtil.DEFAULT_INSTANCE.writeValueAsString(value));
    }

    @Override
    public Object getNullableResult(ResultSet resultSet, String columnName) throws SQLException {
        return read(resultSet.getString(columnName));
    }

    @Override
    public Object getNullableResult(ResultSet resultSet, int columnIndex) throws SQLException {
        return read(resultSet.getString(columnIndex));
    }

    @Override
    public Object getNullableResult(CallableStatement statement, int columnIndex) throws SQLException {
        return read(statement.getString(columnIndex));
    }

    @SneakyThrows
    private Object read(String value) {
        if (value == null) return null;
        value = StrUtil.removePrefix(value, "\"");
        value = StrUtil.removeSuffix(value, "\"");
        return JsonUtil.DEFAULT_INSTANCE.readValue(value, Object.class);
    }
}
