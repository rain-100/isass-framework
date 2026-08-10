// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.database.mybatisplus.mysql;

import cn.hutool.core.util.StrUtil;
import tools.jackson.databind.JsonNode;
import lombok.SneakyThrows;
import vip.isass.framework.common.support.JsonUtil;
import vip.isass.framework.database.mybatisplus.typehandler.IJsonNodeTypeHandler;

import java.sql.PreparedStatement;

public class MysqlJsonNodeTypeHandler implements IJsonNodeTypeHandler {

    @Override
    public String getSupportDatabaseProductName() {
        return "MySQL";
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, JsonNode parameter) {
        doSetNonNullParameter(ps, i, parameter);
    }


    @SneakyThrows
    public static void doSetNonNullParameter(PreparedStatement ps, int i, JsonNode parameter) {
        ps.setString(i, JsonUtil.DEFAULT_INSTANCE.writeValueAsString(parameter));
    }

    @Override
    public JsonNode getJson(String value) {
        return doGetJson(value);
    }

    @SneakyThrows
    public static JsonNode doGetJson(String value) {
        if (value == null) {
            return null;
        }
        value = StrUtil.removePrefix(value, "\"");
        value = StrUtil.removeSuffix(value, "\"");
        return JsonUtil.DEFAULT_INSTANCE.readTree(value);
    }

}
