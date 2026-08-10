// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.database.mybatisplus.typehandler;

import tools.jackson.databind.JsonNode;

import java.sql.PreparedStatement;

public interface IJsonNodeTypeHandler {

    String getSupportDatabaseProductName();

    /**
     * 支持的数据库名称
     */
    default boolean support(String databaseProductName) {
        return getSupportDatabaseProductName().equalsIgnoreCase(databaseProductName);
    }

    void setNonNullParameter(PreparedStatement ps, int i, JsonNode parameter);

    JsonNode getJson(String value);

}
