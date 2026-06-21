package vip.isass.framework.database.core.liquibase;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LiquibaseServiceNamingTest {

    private final LiquibaseServiceNaming naming = new LiquibaseServiceNaming("attachment-service");

    @Test
    void usesDefaultServiceChangeLogWhenChangeLogIsBlank() {
        assertThat(naming.serviceChangeLog(null))
                .isEqualTo("classpath:/db/changelog/attachment-service/db.changelog-master.yaml");
        assertThat(naming.serviceChangeLog("   "))
                .isEqualTo("classpath:/db/changelog/attachment-service/db.changelog-master.yaml");
    }

    @Test
    void appendsServiceDirectoryWhenChangeLogIsDirectory() {
        assertThat(naming.serviceChangeLog("classpath:/db/changelog/"))
                .isEqualTo("classpath:/db/changelog/attachment-service/db.changelog-master.yaml");
        assertThat(naming.serviceChangeLog("classpath:/custom/changelog"))
                .isEqualTo("classpath:/custom/changelog/attachment-service/db.changelog-master.yaml");
    }

    @Test
    void insertsServiceDirectoryBeforeChangeLogFileName() {
        assertThat(naming.serviceChangeLog("classpath:/db/changelog/db.changelog-master.yaml"))
                .isEqualTo("classpath:/db/changelog/attachment-service/db.changelog-master.yaml");
        assertThat(naming.serviceChangeLog("db.changelog-master.yaml"))
                .isEqualTo("attachment-service/db.changelog-master.yaml");
        assertThat(naming.serviceChangeLog("classpath:/db/changelog/master.SQL"))
                .isEqualTo("classpath:/db/changelog/attachment-service/master.SQL");
    }

    @Test
    void prefixesLiquibaseHistoryTablesWithServiceName() {
        assertThat(naming.serviceTableName("databasechangelog"))
                .isEqualTo("attachment-service_databasechangelog");
        assertThat(naming.serviceTableName("databasechangeloglock"))
                .isEqualTo("attachment-service_databasechangeloglock");
    }
}
