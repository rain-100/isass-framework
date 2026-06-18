package vip.isass.framework.database.core.flyway;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.boot.flyway.autoconfigure.FlywayConfigurationCustomizer;
import org.springframework.boot.flyway.autoconfigure.FlywayProperties;
import org.springframework.core.io.ResourceLoader;

import javax.sql.DataSource;
import java.util.List;

public abstract class AbstractFlywayConfiguration {

    protected abstract String getServiceName();

    protected Flyway createFlyway(FlywayProperties properties, DataSource dataSource, ResourceLoader resourceLoader,
                                  List<FlywayConfigurationCustomizer> customizers) {
        FluentConfiguration config = FlywayConfigurer.configure(properties, dataSource, resourceLoader.getClassLoader());

        config.locations(properties.getLocations()
                .stream()
                .map(l -> l + "/" + getServiceName())
                .toArray(String[]::new));

        config.table(getServiceName() + "_" + properties.getTable());

        // cleanOnValidationError 由 spring.flyway.clean-on-validation-error 控制
        // cleanDisabled 由 spring.flyway.clean-disabled 控制（默认 true，生产安全）
        // 上述两个属性已由 FlywayConfigurer 从 FlywayProperties 映射

        String url = properties.getUrl();
        if (url != null && url.contains(":dm:")) {
            config.driver("dm.jdbc.driver.DmDriver");
        }

        customizers.forEach(c -> c.customize(config));

        return config.load();
    }

}
