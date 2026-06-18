package vip.isass.framework.database.core.liquibase;

import liquibase.integration.spring.SpringLiquibase;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseProperties;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.util.Locale;
import java.util.Set;

public abstract class AbstractLiquibaseConfiguration {

    private static final String DEFAULT_CHANGELOG_FILE = "db.changelog-master.yaml";

    private static final Set<String> CHANGELOG_EXTENSIONS = Set.of(".xml", ".yaml", ".yml", ".json", ".sql");

    protected abstract String getServiceName();

    protected SpringLiquibase createLiquibase(LiquibaseProperties properties, DataSource dataSource,
                                              ResourceLoader resourceLoader) {
        SpringLiquibase liquibase = LiquibaseConfigurer.configure(properties, dataSource);
        liquibase.setResourceLoader(resourceLoader);
        liquibase.setChangeLog(serviceChangeLog(properties.getChangeLog()));
        liquibase.setDatabaseChangeLogTable(serviceTableName(properties.getDatabaseChangeLogTable()));
        liquibase.setDatabaseChangeLogLockTable(serviceTableName(properties.getDatabaseChangeLogLockTable()));
        return liquibase;
    }

    private String serviceChangeLog(String changeLog) {
        if (!StringUtils.hasText(changeLog)) {
            return servicePath("classpath:/db/changelog", DEFAULT_CHANGELOG_FILE);
        }
        String normalized = changeLog.trim();
        if (normalized.endsWith("/")) {
            return servicePath(normalized.substring(0, normalized.length() - 1), DEFAULT_CHANGELOG_FILE);
        }
        if (!hasChangeLogExtension(normalized)) {
            return servicePath(normalized, DEFAULT_CHANGELOG_FILE);
        }
        int index = normalized.lastIndexOf('/');
        if (index < 0) {
            return getServiceName() + "/" + normalized;
        }
        return normalized.substring(0, index) + "/" + getServiceName() + normalized.substring(index);
    }

    private String servicePath(String basePath, String fileName) {
        return basePath + "/" + getServiceName() + "/" + fileName;
    }

    private boolean hasChangeLogExtension(String changeLog) {
        String lowerCase = changeLog.toLowerCase(Locale.ROOT);
        return CHANGELOG_EXTENSIONS.stream().anyMatch(lowerCase::endsWith);
    }

    private String serviceTableName(String tableName) {
        return getServiceName() + "_" + tableName;
    }
}
