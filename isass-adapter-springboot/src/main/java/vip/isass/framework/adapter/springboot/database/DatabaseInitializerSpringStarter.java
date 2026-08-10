// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.adapter.springboot.database;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import java.lang.reflect.Method;

/**
 * Spring Boot 数据库初始化入口。
 *
 * @author rain
 */
@Slf4j
public class DatabaseInitializerSpringStarter implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final String DATABASE_INITIALIZER_MANAGER =
            "vip.isass.framework.database.core.init.DatabaseInitializerManager";

    private static volatile boolean RUN = false;

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        if (RUN) {
            return;
        }
        RUN = true;

        Class<?> managerClass = findDatabaseInitializerManager();
        if (managerClass == null) {
            log.info("当前项目没有引入数据库模块，跳过数据库初始化");
            return;
        }

        Environment environment = applicationContext.getEnvironment();
        String autoCreate = environment.getProperty("spring.datasource.autoCreate");
        if ("false".equalsIgnoreCase(autoCreate)) {
            return;
        }

        String jdbcUrl = environment.getProperty("spring.datasource.dynamic.datasource.master.url");
        String username = environment.getProperty("spring.datasource.dynamic.datasource.master.username");
        String password = environment.getProperty("spring.datasource.dynamic.datasource.master.password");
        if (hasBlank(jdbcUrl, username, password)) {
            return;
        }

        try {
            log.info("开始创建数据库: 数据库不存在则自动创建数据库");
            Method initDatabase = managerClass.getMethod("initDatabase", String.class, String.class, String.class);
            initDatabase.invoke(null, jdbcUrl, username, password);
        } catch (ReflectiveOperationException e) {
            log.info("数据库初始化失败");
            log.error(e.getMessage(), e);
        }
    }

    private Class<?> findDatabaseInitializerManager() {
        try {
            return Class.forName(DATABASE_INITIALIZER_MANAGER);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private boolean hasBlank(String... values) {
        for (String value : values) {
            if (value == null || value.isBlank()) {
                return true;
            }
        }
        return false;
    }
}
