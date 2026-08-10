// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.database.mybatisplus.typehandler;

import cn.hutool.core.util.StrUtil;
import tools.jackson.databind.JsonNode;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import vip.isass.framework.common.support.JsonUtil;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ServiceLoader;

/**
 * 处理字段类型为 Json 的数据库映射关系
 *
 * @author Rain
 */
@Slf4j
@MappedJdbcTypes(JdbcType.JAVA_OBJECT)
@MappedTypes({JsonNode.class})
public class JsonNodeTypeHandler extends BaseTypeHandler<JsonNode> {

    @Override
    @SneakyThrows
    public void setNonNullParameter(PreparedStatement ps, int i, JsonNode parameter, JdbcType jdbcType) throws SQLException {
        String databaseProductName = ps.getConnection().getMetaData().getDatabaseProductName();

        ServiceLoader<IJsonNodeTypeHandler> loader = ServiceLoader.load(IJsonNodeTypeHandler.class);
        for (IJsonNodeTypeHandler typeHandler : loader) {
            if (typeHandler.support(databaseProductName)) {
                typeHandler.setNonNullParameter(ps, i, parameter);
                return;
            }
        }

        // 如果没有处理器，则兜底使用字符串处理
        ps.setString(i, JsonUtil.DEFAULT_INSTANCE.writeValueAsString(parameter));
    }

    @Override
    public JsonNode getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return getJson(rs.getString(columnName));
    }

    @Override
    public JsonNode getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return getJson(rs.getString(columnIndex));
    }

    @Override
    public JsonNode getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return getJson(cs.getString(columnIndex));
    }

    @SneakyThrows
    private JsonNode getJson(String value) {
        if (value == null) {
            return null;
        }
        value = StrUtil.removePrefix(value, "\"");
        value = StrUtil.removeSuffix(value, "\"");
        return JsonUtil.DEFAULT_INSTANCE.readTree(value);
    }

}
