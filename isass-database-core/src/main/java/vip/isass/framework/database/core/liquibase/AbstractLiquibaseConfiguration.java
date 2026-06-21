package vip.isass.framework.database.core.liquibase;

import liquibase.integration.spring.SpringLiquibase;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseProperties;
import org.springframework.core.io.ResourceLoader;

import javax.sql.DataSource;

public abstract class AbstractLiquibaseConfiguration {

    protected abstract String getServiceName();

    protected SpringLiquibase createLiquibase(LiquibaseProperties properties, DataSource dataSource,
                                              ResourceLoader resourceLoader) {
        LiquibaseServiceNaming naming = new LiquibaseServiceNaming(getServiceName());
        SpringLiquibase liquibase = LiquibaseConfigurer.configure(properties, dataSource);
        liquibase.setResourceLoader(resourceLoader);
        liquibase.setChangeLog(naming.serviceChangeLog(properties.getChangeLog()));
        liquibase.setDatabaseChangeLogTable(naming.serviceTableName(properties.getDatabaseChangeLogTable()));
        liquibase.setDatabaseChangeLogLockTable(naming.serviceTableName(properties.getDatabaseChangeLogLockTable()));
        return liquibase;
    }
}
