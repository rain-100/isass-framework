// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.database.core.init;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Db;
import cn.hutool.db.StatementUtil;
import cn.hutool.db.ds.simple.SimpleDataSource;
import cn.hutool.db.sql.SqlExecutor;
import lombok.extern.slf4j.Slf4j;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据库初始化管理器
 * jdbcUrl 指定的数据库不存在时自动创建数据库
 *
 * @author rain
 */
@Slf4j
public class DatabaseInitializerManager {

    /**
     * 定义可能的 schema 参数名
     */
    public static final String[] SCHEMA_PARAM_NAMES = {"schema", "SCHEMA", "currentSchema"};

    public static String DATABASE_NAME = "";

    public static String SCHEMA_NAME = "";

    public static volatile boolean INITIALIZED = false;

    public static void initDatabase(String jdbcUrl, String username, String password) {
        try {
            log.info("Start creating database if the database does not exist");

            if (tryConnect(jdbcUrl, username, password)) {
                return;
            }

            // 解析数据库名和schema名
            DATABASE_NAME = parseDatabaseName(jdbcUrl);
            SCHEMA_NAME = parseSchemaName(jdbcUrl);
            log.info("database name: {}", DATABASE_NAME);
            log.info("schema name: {}", SCHEMA_NAME);

            // 如果有 schemaName，则尝试创建，创建成功，代表数据库已存在，则数据库初始化逻辑完成
            if (StrUtil.isNotBlank(SCHEMA_NAME)) {
                // jdbcUrl 去除 schema,否则 jdbc 连接不上
                String tempJdbcUrl = removeJdbcUrlSchemaName(jdbcUrl);
                if (execSql(tempJdbcUrl, username, password, getCreateSchemaSql(jdbcUrl, SCHEMA_NAME))) {
                    return;
                }
            }

            if (StrUtil.isBlank(DATABASE_NAME)) {
                return;
            }

            // 处理 jdbcUrl 的 databaseName(去除或替换成默认数据库名) 和 schema(去除),否则 jdbc 连接不上
            String tempJdbcUrl = replaceDatabaseName(jdbcUrl);
            tempJdbcUrl = removeJdbcUrlSchemaName(tempJdbcUrl);
            if (execSql(tempJdbcUrl, username, password, getCreateDatabaseSql(jdbcUrl, DATABASE_NAME))) {
                // 数据库创建成功，如果 schemaName 不存在，则完成初始化
                if (StrUtil.isBlank(SCHEMA_NAME)) {
                    return;
                }

                // 尝试连接，因为 schemaName 可能是数据库会默认创建的，不用手工创建
                if (tryConnect(jdbcUrl, username, password)) {
                    return;
                }

                // 数据库创建成功了，但是连接不成功，代表 schema 需要手工创建
                // jdbcUrl 去除 schema,否则 jdbc 连接不上
                tempJdbcUrl = removeJdbcUrlSchemaName(jdbcUrl);
                execSql(tempJdbcUrl, username, password, getCreateSchemaSql(jdbcUrl, SCHEMA_NAME));
            }
        } finally {
            INITIALIZED = true;
        }
    }

    private static boolean tryConnect(String jdbcUrl, String username, String password) {
        log.info("try connecting datasource using jdbcUrl: {}", jdbcUrl);

        // 尝试连接
        try (SimpleDataSource ds = new SimpleDataSource(jdbcUrl, username, password);
             Connection conn = ds.getConnection();) {
            log.info("datasource connect success, skip database initialization");
            return true;
        } catch (Exception e) {
            log.info("try connecting database fail: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 根据 jdbcUrl 解析出数据库名
     * 示例: jdbc:mysql://127.0.0.1:3306/attachment?useUnicode
     * 示例: jdbc:dm://172.25.23.66:5236?schema=test&stringtype=unspecified
     * 从提取到 attachment 作为数据库名，如果无法解析则返回null
     */
    private static String parseDatabaseName(String jdbcUrl) {
        int index = jdbcUrl.indexOf("://");
        if (index == -1) {
            log.warn("can not parse database name from jdbcUrl: {}", jdbcUrl);
            return null;
        }

        int slashIndex = jdbcUrl.indexOf("/", index + 3);
        if (slashIndex == -1) {
            log.warn("can not parse database name from jdbcUrl: {}", jdbcUrl);
            return null;
        }

        slashIndex++;
        if (slashIndex >= jdbcUrl.length()) {
            log.warn("can not parse database name from jdbcUrl: {}", jdbcUrl);
            return null;
        }

        int questionMarkIndex = jdbcUrl.indexOf("?", slashIndex);
        String databaseName = questionMarkIndex == -1
                ? jdbcUrl.substring(slashIndex)
                : jdbcUrl.substring(slashIndex, questionMarkIndex);

        if (StrUtil.isBlank(databaseName)) {
            log.warn("can not parse database name from jdbcUrl: {}", jdbcUrl);
            return null;
        }
        return databaseName;
    }

    /**
     * 根据 jdbcUrl 解析出 schemaName
     * 检查jdbcUrl是否包含"schema="（忽略大小写），如果包含则提取schema值
     * 示例: jdbc:postgresql://172.25.23.66:54321/test?currentSchema=auth&stringtype=unspecified
     * 示例: jdbc:dm://172.25.23.66:5236?schema=test&stringtype=unspecified
     * 从提取到 auth 或 test 作为 schemaName
     */
    private static String parseSchemaName(String jdbcUrl) {
        // 分割 URL 为基本部分 and 查询字符串
        String[] parts = jdbcUrl.split("\\?", 2);
        if (parts.length < 2) {
            // 问号后面没有参数，即 jdbcUrl 本身没写 schema 参数,直接返回即可
            return null;
        }

        try {
            // 解析查询字符串为参数映射
            Map<String, String> queryParams = parseQueryString(parts[1]);

            for (String schemaParam : SCHEMA_PARAM_NAMES) {
                String schema = queryParams.get(schemaParam);
                if (schema != null) {
                    return schema;
                }
            }

            return null;
        } catch (Exception e) {
            log.warn("Failed to parse schema name in jdbcUrl: {}", jdbcUrl, e);
            return null;
        }
    }

    private static Map<String, String> parseQueryString(String queryString) {
        Map<String, String> params = new HashMap<>();
        String[] pairs = queryString.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            String value = keyValue.length == 1
                    ? ""
                    : URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
            params.put(keyValue[0], value);
        }
        return params;
    }

    private static boolean execSql(String jdbcUrl, String username, String password, String sql) {
        Db db = null;
        Connection conn = null;
        PreparedStatement preparedStatement = null;
        try (SimpleDataSource ds = new SimpleDataSource(jdbcUrl, username, password)) {
            db = Db.use(ds);
            conn = ds.getConnection();
            log.info("exec sql: {}", sql);
            preparedStatement = StatementUtil.prepareStatement(conn, sql);
            SqlExecutor.execute(preparedStatement);
            log.info("sql exec success");
            return true;
        } catch (Exception e) {
            log.info("sql exec fail: {}", e.getMessage());
            return false;
        } finally {
            if (db != null) {
                db.closeConnection(conn);
            } else {
                IoUtil.close(conn);
            }
            IoUtil.close(preparedStatement);
        }
    }

    private static String getCreateDatabaseSql(String jdbcUrl, String databaseName) {
        return String.format("CREATE DATABASE %s", databaseName);
    }

    private static String getCreateSchemaSql(String jdbcUrl, String schemaName) {
        return String.format("CREATE SCHEMA %s", schemaName);
    }

    /**
     * 删除 jdbcUrl 的数据库名
     */
    private static String replaceDatabaseName(String jdbcUrl) {
        if (jdbcUrl.contains(":postgresql:")) {
            // PostgreSQL 的 jdbcUrl 格式: jdbc:postgresql://host:port/database
            // 需要将数据库名替换为默认的 postgres
            return jdbcUrl.replace("/" + DATABASE_NAME, "/postgres");
        }

        if (jdbcUrl.contains(":kingbase")) {
            // Kingbase 的 jdbcUrl 格式: jdbc:kingbase8://host:port/database
            // 需要将数据库名替换为默认的 kingbase
            return jdbcUrl.replace("/" + DATABASE_NAME, "/kingbase");
        }

        if (jdbcUrl.contains(":highgo:")) {
            // 瀚高 的 jdbcUrl 格式: jdbc:highgo://localhost:5866/mydatabase?currentSchema=myschema
            // 需要将数据库名替换为默认的 highgo
            return jdbcUrl.replace("/" + DATABASE_NAME, "/highgo");
        }

        // 其他未特殊处理的数据库，直接删除数据库名
        return jdbcUrl.replace("/" + DATABASE_NAME, "");
    }

    /**
     * 示例: jdbc:postgresql://172.25.23.66:54321/test?currentSchema=auth&stringtype=unspecified
     */
    private static String removeJdbcUrlSchemaName(String jdbcUrl) {
        // 分割 URL 为基本部分 and 查询字符串
        String[] parts = jdbcUrl.split("\\?", 2);
        if (parts.length < 2) {
            // 问号后面没有参数，即 jdbcUrl 本身没写 schema 参数,直接返回即可
            return jdbcUrl;
        }

        String baseUrl = parts[0];

        try {
            // 解析查询字符串为参数映射
            Map<String, String> queryParams = parseQueryString(parts[1]);

            // 移除所有可能的schema参数
            for (String schemaParam : SCHEMA_PARAM_NAMES) {
                queryParams.remove(schemaParam);
            }

            // 重新构建查询字符串
            StringBuilder newQueryString = new StringBuilder();
            for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                if (newQueryString.length() > 0) {
                    newQueryString.append("&");
                }
                newQueryString.append(entry.getKey()).append("=").append(entry.getValue());
            }

            // 重新构建URL
            return newQueryString.length() == 0 ? baseUrl : baseUrl + "?" + newQueryString;
        } catch (Exception e) {
            log.warn("Failed to replace schema name in jdbcUrl: {}", jdbcUrl, e);
            return jdbcUrl;
        }
    }
}
