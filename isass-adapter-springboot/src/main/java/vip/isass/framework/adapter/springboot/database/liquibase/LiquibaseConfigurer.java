// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.adapter.springboot.database.liquibase;

import liquibase.UpdateSummaryEnum;
import liquibase.UpdateSummaryOutputEnum;
import liquibase.integration.spring.SpringLiquibase;
import liquibase.ui.UIServiceEnum;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseProperties;

import javax.sql.DataSource;
import java.util.Collection;

public final class LiquibaseConfigurer {

    private LiquibaseConfigurer() {
    }

    public static SpringLiquibase configure(LiquibaseProperties properties, DataSource dataSource) {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog(properties.getChangeLog());
        liquibase.setClearCheckSums(properties.isClearChecksums());
        if (!isEmpty(properties.getContexts())) {
            liquibase.setContexts(collectionToCommaDelimitedString(properties.getContexts()));
        }
        liquibase.setDefaultSchema(properties.getDefaultSchema());
        liquibase.setLiquibaseSchema(properties.getLiquibaseSchema());
        liquibase.setLiquibaseTablespace(properties.getLiquibaseTablespace());
        liquibase.setDatabaseChangeLogTable(properties.getDatabaseChangeLogTable());
        liquibase.setDatabaseChangeLogLockTable(properties.getDatabaseChangeLogLockTable());
        liquibase.setDropFirst(properties.isDropFirst());
        liquibase.setShouldRun(properties.isEnabled());
        if (!isEmpty(properties.getLabelFilter())) {
            liquibase.setLabelFilter(collectionToCommaDelimitedString(properties.getLabelFilter()));
        }
        liquibase.setChangeLogParameters(properties.getParameters());
        liquibase.setRollbackFile(properties.getRollbackFile());
        liquibase.setTestRollbackOnUpdate(properties.isTestRollbackOnUpdate());
        liquibase.setTag(properties.getTag());
        if (properties.getShowSummary() != null) {
            liquibase.setShowSummary(UpdateSummaryEnum.valueOf(properties.getShowSummary().name()));
        }
        if (properties.getShowSummaryOutput() != null) {
            liquibase.setShowSummaryOutput(UpdateSummaryOutputEnum.valueOf(properties.getShowSummaryOutput().name()));
        }
        if (properties.getUiService() != null) {
            liquibase.setUiService(UIServiceEnum.valueOf(properties.getUiService().name()));
        }
        if (properties.getAnalyticsEnabled() != null) {
            liquibase.setAnalyticsEnabled(properties.getAnalyticsEnabled());
        }
        if (properties.getLicenseKey() != null) {
            liquibase.setLicenseKey(properties.getLicenseKey());
        }
        return liquibase;
    }

    private static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    private static String collectionToCommaDelimitedString(Collection<String> collection) {
        return String.join(",", collection);
    }
}
