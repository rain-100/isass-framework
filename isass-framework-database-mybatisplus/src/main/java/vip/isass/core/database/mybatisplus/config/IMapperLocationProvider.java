package vip.isass.core.database.mybatisplus.config;

import java.util.List;

/**
 * <p>用于配置 mybatis 的 MapperLocations</p>
 * <p>各微服务实现此接口，无需再通过配置项 <code> mybatis-plus.mapper-locations </code> 进行配置</p>
 * <p>解决不同 package 的微服务打包到单体时，配置被覆盖的问题</p>
 *
 * @author rain
 */
public interface IMapperLocationProvider {

    /**
     * @return MapperLocation list
     */
    List<String> getMapperLocations();

}
