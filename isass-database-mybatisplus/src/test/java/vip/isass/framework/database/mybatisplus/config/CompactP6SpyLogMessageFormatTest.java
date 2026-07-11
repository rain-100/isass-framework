package vip.isass.framework.database.mybatisplus.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CompactP6SpyLogMessageFormatTest {

    private final CompactP6SpyLogMessageFormat format = new CompactP6SpyLogMessageFormat();

    @Test
    void logsOnlyEffectiveSqlAndCompactMysqlAddress() {
        String message = format.formatMessage(
                4,
                "2026-07-10 23:35:09",
                6,
                "statement",
                "SELECT id FROM att_icon_group WHERE id = ?",
                "SELECT id FROM att_icon_group WHERE id = 9",
                "jdbc:mysql://192.168.2.10:3306/attachment?useUnicode=true&characterEncoding=utf8");

        assertEquals("6ms|mysql://192.168.2.10:3306/attachment|SELECT id FROM att_icon_group WHERE id = 9",
                message);
    }

    @Test
    void removesNonSqlCategoriesAndSqlLineBreaks() {
        String message = format.formatMessage(
                4,
                "2026-07-10 23:35:09",
                6,
                "statement",
                "SELECT ?",
                "SELECT\n  id\nFROM att_icon_group",
                "jdbc:postgresql://127.0.0.1:5432/attachment;currentSchema=public");

        assertEquals("6ms|postgresql://127.0.0.1:5432/attachment|SELECT id FROM att_icon_group", message);
    }
}
