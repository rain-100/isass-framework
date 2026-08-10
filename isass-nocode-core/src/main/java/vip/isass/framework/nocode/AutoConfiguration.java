// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vip.isass.framework.nocode.service.IService;
import vip.isass.framework.nocode.service.ILocalService;
import vip.isass.framework.nocode.service.ILocalApplicationService;
import vip.isass.framework.nocode.transport.ServiceProxyRegistrar;

import java.util.List;

@Configuration
public class AutoConfiguration {

    /**
     * nocode 服务注册表：启动时仅扫描本地 {@link ILocalService} Bean，构建 entity → IService 映射。
     * 远程 {@link IService} 代理属于调用方能力，不能作为本服务的 HTTP/gRPC 服务端合同注册；
     * 否则动态代理没有可反射的实体泛型，会在启动时解析失败。
     * 注意：表元数据 {@link TableMeta} 由 {@link TableMetaRegistrar} 在 BDRPP 阶段独立完成，
     * 不依赖本 Bean 的初始化时机，避免与 {@code SqlSessionFactory} 的创建顺序冲突。
     */
    @Bean
    public ServiceRegistry ServiceRegistry(
            List<ILocalService<?, ?>> services,
            List<ILocalApplicationService> applicationServices
    ) {
        return new ServiceRegistry(services, applicationServices);
    }

    /** Kept for programmatic callers that only register standard CRUD services. */
    public ServiceRegistry ServiceRegistry(List<ILocalService<?, ?>> services) {
        return new ServiceRegistry(services);
    }

    /**
     * {@link TableMetaRegistrar} 在后置处理阶段扫描 {@link vip.isass.framework.nocode.entity.IEntity}
     * 的所有实现类并填充元数据，保证在 MyBatis-Plus 构建 {@code TableInfo} 之前完成。
     */
    @Bean
    public TableMetaRegistrar TableMetaRegistrar() {
        return new TableMetaRegistrar();
    }

    /** Registers typed remote proxies for V4 service contracts with no local implementation. */
    @Bean
    public static ServiceProxyRegistrar ServiceProxyRegistrar() {
        return new ServiceProxyRegistrar();
    }
}
