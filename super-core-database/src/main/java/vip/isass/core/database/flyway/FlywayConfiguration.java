package vip.isass.core.database.flyway;

import org.flywaydb.core.Flyway;
import org.springframework.boot.flyway.autoconfigure.FlywayProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * 定制化 Flyway 配置，支持为不同模块（微服务）配置独立的 Flyway 实例。
 * 适用于单体启动时管理多个微服务的数据库迁移。
 */
@Configuration
public class FlywayConfiguration {

    /**
     * 创建默认的 Flyway 实例（用于主服务）
     */
    @Bean
    public Flyway flyway(DataSource dataSource, FlywayProperties properties, ResourceLoader resourceLoader) {
        return createFlywayInstance(dataSource, properties, "db/migration", "flyway_schema_history", resourceLoader);
    }

    /**
     * 为特定模块（如微服务）创建独立的 Flyway 实例
     * @param moduleName 模块名称（对应 sql 脚本目录和表名后缀）
     */
    public Flyway createModuleFlyway(DataSource dataSource, FlywayProperties properties, String moduleName, ResourceLoader resourceLoader) {
        String locations = "db/migration/" + moduleName;
        String table = "flyway_schema_history_" + moduleName;
        return createFlywayInstance(dataSource, properties, locations, table, resourceLoader);
    }

    private Flyway createFlywayInstance(DataSource dataSource, FlywayProperties properties, String locations, String table, ResourceLoader resourceLoader) {
        org.flywaydb.core.api.configuration.FluentConfiguration config = Flyway.configure(resourceLoader.getClassLoader())
                .dataSource(dataSource)
                .locations(locations)
                .table(table)
                .baselineOnMigrate(properties.isBaselineOnMigrate())
                .validateOnMigrate(properties.isValidateOnMigrate());
        
        // 处理达梦数据库等特殊配置
        if (properties.getUrl() != null && properties.getUrl().contains("dm")) {
            config.driver("dm.jdbc.driver.DmDriver");
        }

        return config.load();
    }
}
