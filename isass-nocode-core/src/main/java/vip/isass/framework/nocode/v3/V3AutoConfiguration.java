package vip.isass.framework.nocode.v3;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vip.isass.framework.nocode.v3.service.IV3Service;

import java.util.List;

@Configuration
public class V3AutoConfiguration {

    /**
     * v3 服务注册表：启动时扫描所有 {@link IV3Service} Bean，构建 entityName → IV3Service 映射。
     * 注意：表元数据 {@link V3TableMeta} 由 {@link V3TableMetaRegistrar} 在 BDRPP 阶段独立完成，
     * 不依赖本 Bean 的初始化时机，避免与 {@code SqlSessionFactory} 的创建顺序冲突。
     */
    @Bean
    public V3ServiceRegistry v3ServiceRegistry(List<IV3Service<?, ?>> services) {
        return new V3ServiceRegistry(services);
    }

    /**
     * {@link V3TableMetaRegistrar} 在后置处理阶段扫描 {@link vip.isass.framework.nocode.v3.entity.IV3Entity}
     * 的所有实现类并填充元数据，保证在 MyBatis-Plus 构建 {@code TableInfo} 之前完成。
     */
    @Bean
    public V3TableMetaRegistrar v3TableMetaRegistrar() {
        return new V3TableMetaRegistrar();
    }

    @Bean
    public V3CriteriaMapper v3CriteriaMapper() {
        return new V3CriteriaMapper();
    }
}
