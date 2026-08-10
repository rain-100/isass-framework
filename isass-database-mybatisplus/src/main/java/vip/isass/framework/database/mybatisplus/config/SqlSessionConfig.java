// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.database.mybatisplus.config;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ArrayUtil;
import com.baomidou.mybatisplus.autoconfigure.ConfigurationCustomizer;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusPropertiesCustomizer;
import com.baomidou.mybatisplus.autoconfigure.SqlSessionFactoryBeanCustomizer;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DynamicTableNameInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.logging.nologging.NoLoggingImpl;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.mapping.VendorDatabaseIdProvider;
import org.apache.ibatis.session.AutoMappingUnknownColumnBehavior;
import org.apache.ibatis.session.LocalCacheScope;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.lang.NonNull;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.TransactionManagementConfigurer;
import vip.isass.framework.database.mybatisplus.plus.handler.MybatisPlusMetaObjectHandler;
import vip.isass.framework.database.mybatisplus.typehandler.enums.ExtendedCompositeEnumTypeHandler;
import vip.isass.framework.common.page.PageConst;

import javax.sql.DataSource;
import java.util.List;
import java.util.Properties;

/**
 * @author rain
 */
@Slf4j
@Configuration
@EnableTransactionManagement
public class SqlSessionConfig implements TransactionManagementConfigurer {

    private final DataSource dataSource;

    private final List<IMapperLocationProvider> mapperLocationProviders;

    private final List<BaseTypeHandler<?>> baseTypeHandlers;

    private final String tableNameStrategy;

    public SqlSessionConfig(DataSource dataSource,
                            ObjectProvider<IMapperLocationProvider> mapperLocationProviders,
                            ObjectProvider<BaseTypeHandler<?>> baseTypeHandlers,
                            @Value("${mybatis-plus.tableNameStrategy:none}") String tableNameStrategy) {
        this.dataSource = dataSource;
        this.mapperLocationProviders = mapperLocationProviders.orderedStream().toList();
        this.baseTypeHandlers = baseTypeHandlers.orderedStream().toList();
        this.tableNameStrategy = tableNameStrategy;
    }

    @Bean
    public DatabaseIdProvider databaseIdProvider() {
        VendorDatabaseIdProvider databaseIdProvider = new VendorDatabaseIdProvider();
        Properties properties = new Properties();
        properties.put("Oracle", "oracle");
        properties.put("MySQL", "mysql");
        properties.put("DM DBMS", "dm");
        properties.put("KingbaseES", "kingBase");
        properties.put("PostgreSQL", "postgresql");
        properties.put("Highgo", "Highgo");
        databaseIdProvider.setProperties(properties);
        return databaseIdProvider;
    }

    @Bean
    public MybatisPlusPropertiesCustomizer mybatisPlusPropertiesCustomizer() {
        return properties -> {
            GlobalConfig globalConfig = properties.getGlobalConfig()
                    .setBanner(false)

                    // 自定义填充策略接口实现
                    .setMetaObjectHandler(new MybatisPlusMetaObjectHandler());

            globalConfig.getDbConfig()
                    .setLogicDeleteValue(Boolean.TRUE.toString())
                    .setLogicNotDeleteValue(Boolean.FALSE.toString());

            if (CollUtil.isNotEmpty(mapperLocationProviders)) {
                String[] array = mapperLocationProviders.stream()
                        .map(IMapperLocationProvider::getMapperLocations)
                        .flatMap(List::stream)
                        .toArray(String[]::new);
                properties.setMapperLocations(ArrayUtil.addAll(properties.getMapperLocations(), array));
            }
        };
    }

    @Bean
    public ConfigurationCustomizer configurationCustomizer() {
        return configuration -> {
            // Mybatis 一级缓存
            // SESSION：Session 级别缓存，同一个 Session 相同查询语句不会再次查询数据库
            // STATEMENT：关闭一级缓存
            // 在单服务架构中（仅有一个程序提供相同服务），开启一级缓存不会影响业务，只会提高性能。
            // 在微服务架构中需要关闭一级缓存，原因是：Service1 查询数据后，如果 Service2 修改了数据，Service1 再次查询时可能会得到过期数据。
            configuration.setLocalCacheScope(LocalCacheScope.STATEMENT);

            // 是否开启 MyBatis 二级缓存。
            configuration.setCacheEnabled(false);

            configuration.setJdbcTypeForNull(JdbcType.NULL);

            // 设置 MyBatis 的日志实现。关闭 mybatis 日志。由 p6spy 实现日志打印
            configuration.setLogImpl(NoLoggingImpl.class);

            // 枚举映射 https://baomidou.com/reference/#defaultenumtypehandler
            configuration.setDefaultEnumTypeHandler(ExtendedCompositeEnumTypeHandler.class);

            /*
             * MyBatis 自动映射时未知列或未知属性处理策略
             * 通过该配置可指定 MyBatis 在自动映射过程中遇到未知列或者未知属性时如何处理，总共有 3 种可选值：
             * AutoMappingUnknownColumnBehavior.NONE：不做任何处理 (默认值)
             * AutoMappingUnknownColumnBehavior.WARNING：以日志的形式打印相关警告信息
             * AutoMappingUnknownColumnBehavior.FAILING：当作映射失败处理，并抛出异常和详细信息
             */
            configuration.setAutoMappingUnknownColumnBehavior(AutoMappingUnknownColumnBehavior.WARNING);
        };
    }

    @Bean
    public SqlSessionFactoryBeanCustomizer sqlSessionFactoryBeanCustomizer() {
        return sqlSessionFactoryBean -> {
            if (CollUtil.isNotEmpty(baseTypeHandlers)) {
                baseTypeHandlers.forEach(sqlSessionFactoryBean::addTypeHandlers);
            }
        };
    }

    @NonNull
    @Override
    public PlatformTransactionManager annotationDrivenTransactionManager() {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 乐观锁插件
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

        // 动态表名插件
        if ("uppercase".equals(tableNameStrategy)) {
            DynamicTableNameInnerInterceptor dynamicTableNameInnerInterceptor = new DynamicTableNameInnerInterceptor();
            dynamicTableNameInnerInterceptor.setTableNameHandler((sql, tableName) -> tableName.toUpperCase());
            interceptor.addInnerInterceptor(dynamicTableNameInnerInterceptor);
        } else if ("lowercase".equals(tableNameStrategy)) {
            DynamicTableNameInnerInterceptor dynamicTableNameInnerInterceptor = new DynamicTableNameInnerInterceptor();
            dynamicTableNameInnerInterceptor.setTableNameHandler((sql, tableName) -> tableName.toLowerCase());
            interceptor.addInnerInterceptor(dynamicTableNameInnerInterceptor);
        } else if (!"none".equals(tableNameStrategy)) {
            log.warn("Unsupported table name strategy: {}, using default behavior.", tableNameStrategy);
        }

        // 如果配置多个插件, 切记分页最后添加
        PaginationInnerInterceptor paginationInnerInterceptor = new PaginationInnerInterceptor();
        paginationInnerInterceptor.setMaxLimit(PageConst.MAX_PAGE_SIZE);
        paginationInnerInterceptor.setOverflow(true);
        interceptor.addInnerInterceptor(paginationInnerInterceptor);
        return interceptor;
    }
}
