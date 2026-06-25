package vip.isass.framework.adapter.springboot.database;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseProperties;
import org.springframework.context.annotation.Bean;
import vip.isass.framework.adapter.springboot.condition.ConditionalOnIsassFeature;
import vip.isass.framework.adapter.springboot.condition.IsassFeature;
import vip.isass.framework.common.exception.IExceptionMapping;

/**
 * Spring Boot bridge for database-core beans that should only exist when database-core is on the classpath.
 */
@AutoConfiguration
@ConditionalOnIsassFeature(IsassFeature.DATABASE_CORE)
@EnableConfigurationProperties(LiquibaseProperties.class)
public class IsassDatabaseSpringBootAutoConfiguration {

    private static final String DATABASE_EXCEPTION_MAPPING_CLASS =
            "vip.isass.framework.database.core.exception.DatabaseExceptionMapping";

    @Bean
    @ConditionalOnMissingBean(name = "databaseExceptionMapping")
    public IExceptionMapping databaseExceptionMapping() {
        try {
            return (IExceptionMapping) Class.forName(DATABASE_EXCEPTION_MAPPING_CLASS)
                    .getDeclaredConstructor()
                    .newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to create database exception mapping", e);
        }
    }
}
