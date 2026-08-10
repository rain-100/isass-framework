// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.database.mybatisplus.postgresql;

import tools.jackson.databind.JsonNode;
import lombok.SneakyThrows;
import org.postgresql.util.PGobject;
import vip.isass.framework.common.support.JsonUtil;
import vip.isass.framework.database.mybatisplus.typehandler.IJsonNodeTypeHandler;

import java.sql.PreparedStatement;

/**
 * 处理字段类型为 Jsonb 的数据库映射关系
 *
 * @author Rain
 */
public class PostgresqlJsonNodeTypeHandler implements IJsonNodeTypeHandler {

    @Override
    public String getSupportDatabaseProductName() {
        return "PostgreSQL";
    }

    @Override
    @SneakyThrows
    public void setNonNullParameter(PreparedStatement ps, int i, JsonNode parameter) {
        PGobject pGobject = new PGobject();
        pGobject.setValue(JsonUtil.DEFAULT_INSTANCE.writeValueAsString(parameter));
        ps.setObject(i, pGobject);
    }

    @Override
    @SneakyThrows
    public JsonNode getJson(String value) {
        if (value == null) {
            return null;
        }
        return JsonUtil.DEFAULT_INSTANCE.readTree(value);
    }

}