package vip.isass.framework.database.mybatisplus.config;

import com.p6spy.engine.spy.appender.MessageFormattingStrategy;

/**
 * P6Spy 单行 SQL 日志格式：仅保留执行耗时、数据库地址和带实参 SQL。
 */
public class CompactP6SpyLogMessageFormat implements MessageFormattingStrategy {

    @Override
    public String formatMessage(
            int connectionId,
            String now,
            long elapsed,
            String category,
            String prepared,
            String sql,
            String url
    ) {
        if (sql == null || sql.isBlank()) {
            return "";
        }
        return elapsed + "ms|" + compactDatabaseUrl(url) + "|" + compactSql(sql);
    }

    private String compactDatabaseUrl(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        String databaseUrl = url.replaceFirst("^jdbc:", "");
        int parameterStart = firstParameterStart(databaseUrl);
        return parameterStart < 0 ? databaseUrl : databaseUrl.substring(0, parameterStart);
    }

    private int firstParameterStart(String value) {
        int queryStart = value.indexOf('?');
        int semicolonStart = value.indexOf(';');
        if (queryStart < 0) {
            return semicolonStart;
        }
        if (semicolonStart < 0) {
            return queryStart;
        }
        return Math.min(queryStart, semicolonStart);
    }

    private String compactSql(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
