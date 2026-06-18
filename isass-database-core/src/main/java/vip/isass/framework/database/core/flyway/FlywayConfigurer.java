package vip.isass.framework.database.core.flyway;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.flyway.autoconfigure.FlywayProperties;

import javax.sql.DataSource;
import java.util.stream.Collectors;

public final class FlywayConfigurer {

    private FlywayConfigurer() {
    }

    public static FluentConfiguration configure(FlywayProperties properties, DataSource dataSource, ClassLoader classLoader) {
        FluentConfiguration config = Flyway.configure(classLoader)
                .dataSource(dataSource);

        PropertyMapper map = PropertyMapper.get();

        map.from(properties::getLocations).to(locations ->
                config.locations(locations.toArray(new String[0])));
        map.from(properties::getCallbackLocations).to(locations ->
                config.callbacks(locations.toArray(new String[0])));
        map.from(properties::getEncoding).to(config::encoding);
        map.from(properties::getConnectRetries).to(config::connectRetries);
        map.from(properties::getConnectRetriesInterval).asInt(java.time.Duration::getSeconds).to(config::connectRetriesInterval);
        map.from(properties::getLockRetryCount).to(config::lockRetryCount);
        map.from(properties::getDefaultSchema).to(config::defaultSchema);
        map.from(properties::getSchemas).to(schemas ->
                config.schemas(schemas.toArray(new String[0])));
        map.from(properties::isCreateSchemas).to(config::createSchemas);
        map.from(properties::getTable).to(config::table);
        map.from(properties::getTablespace).to(config::tablespace);
        map.from(properties::getBaselineDescription).to(config::baselineDescription);
        map.from(properties::getBaselineVersion).to(config::baselineVersion);
        map.from(properties::getInstalledBy).to(config::installedBy);
        map.from(properties::getPlaceholders).to(config::placeholders);
        map.from(properties::getPlaceholderPrefix).to(config::placeholderPrefix);
        map.from(properties::getPlaceholderSuffix).to(config::placeholderSuffix);
        map.from(properties::getPlaceholderSeparator).to(config::placeholderSeparator);
        map.from(properties::isPlaceholderReplacement).to(config::placeholderReplacement);
        map.from(properties::getSqlMigrationPrefix).to(config::sqlMigrationPrefix);
        map.from(properties::getSqlMigrationSuffixes).to(suffixes ->
                config.sqlMigrationSuffixes(suffixes.toArray(new String[0])));
        map.from(properties::getSqlMigrationSeparator).to(config::sqlMigrationSeparator);
        map.from(properties::getRepeatableSqlMigrationPrefix).to(config::repeatableSqlMigrationPrefix);
        map.from(properties::getTarget).to(config::target);
        map.from(properties::isBaselineOnMigrate).to(config::baselineOnMigrate);
        map.from(properties::isCleanDisabled).to(config::cleanDisabled);
        map.from(properties::isGroup).to(config::group);
        map.from(properties::isMixed).to(config::mixed);
        map.from(properties::isOutOfOrder).to(config::outOfOrder);
        map.from(properties::isSkipDefaultCallbacks).to(config::skipDefaultCallbacks);
        map.from(properties::isSkipDefaultResolvers).to(config::skipDefaultResolvers);
        map.from(properties::isValidateMigrationNaming).to(config::validateMigrationNaming);
        map.from(properties::isValidateOnMigrate).to(config::validateOnMigrate);
        map.from(properties::getInitSqls).as(initSqls ->
                String.join("\n", initSqls)).to(config::initSql);
        map.from(properties::getScriptPlaceholderPrefix).to(config::scriptPlaceholderPrefix);
        map.from(properties::getScriptPlaceholderSuffix).to(config::scriptPlaceholderSuffix);
        map.from(properties::isExecuteInTransaction).to(config::executeInTransaction);
        map.from(properties::getLoggers).to(config::loggers);
        map.from(properties::getBatch).to(config::batch);
        map.from(properties::getDryRunOutput).to(config::dryRunOutput);
        map.from(properties::getErrorOverrides).to(config::errorOverrides);
        map.from(properties::getStream).to(config::stream);
        map.from(properties::getJdbcProperties).to(config::jdbcProperties);
        map.from(properties::getKerberosConfigFile).to(config::kerberosConfigFile);
        map.from(properties::getOutputQueryResults).to(config::outputQueryResults);
        map.from(properties::getSkipExecutingMigrations).to(config::skipExecutingMigrations);
        map.from(properties::getIgnoreMigrationPatterns).to(patterns ->
                config.ignoreMigrationPatterns(patterns.toArray(new String[0])));
        map.from(properties::getDetectEncoding).to(config::detectEncoding);
        map.from(properties::getCommunityDbSupportEnabled).to(config::communityDBSupportEnabled);
        map.from(properties::isFailOnMissingLocations).to(config::failOnMissingLocations);

        return config;
    }
}
