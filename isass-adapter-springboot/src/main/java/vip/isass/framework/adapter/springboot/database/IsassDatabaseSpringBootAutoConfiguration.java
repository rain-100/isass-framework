package vip.isass.framework.adapter.springboot.database;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import vip.isass.framework.common.exception.IExceptionMapping;

/**
 * Spring Boot bridge for database-core beans that should only exist when database-core is on the classpath.
 */
@AutoConfiguration
@ConditionalOnClass(name = "vip.isass.framework.database.core.exception.DatabaseExceptionMapping")
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
