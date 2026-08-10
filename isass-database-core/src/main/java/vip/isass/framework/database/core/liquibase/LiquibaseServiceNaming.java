// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.database.core.liquibase;

import java.util.Locale;
import java.util.Set;

/**
 * Pure Java naming rules for service-scoped Liquibase resources and history tables.
 */
public final class LiquibaseServiceNaming {

    static final String DEFAULT_CHANGELOG_FILE = "db.changelog-master.yaml";

    private static final Set<String> CHANGELOG_EXTENSIONS = Set.of(".xml", ".yaml", ".yml", ".json", ".sql");

    private final String serviceName;

    public LiquibaseServiceNaming(String serviceName) {
        this.serviceName = serviceName;
    }

    public String serviceChangeLog(String changeLog) {
        if (!hasText(changeLog)) {
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
            return serviceName + "/" + normalized;
        }
        return normalized.substring(0, index) + "/" + serviceName + normalized.substring(index);
    }

    public String serviceTableName(String tableName) {
        return serviceName + "_" + tableName;
    }

    private String servicePath(String basePath, String fileName) {
        return basePath + "/" + serviceName + "/" + fileName;
    }

    private boolean hasChangeLogExtension(String changeLog) {
        String lowerCase = changeLog.toLowerCase(Locale.ROOT);
        return CHANGELOG_EXTENSIONS.stream().anyMatch(lowerCase::endsWith);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
