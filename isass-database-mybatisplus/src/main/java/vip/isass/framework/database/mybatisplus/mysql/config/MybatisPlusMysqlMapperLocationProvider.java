package vip.isass.framework.database.mybatisplus.mysql.config;

import org.springframework.stereotype.Component;
import vip.isass.framework.database.mybatisplus.config.IMapperLocationProvider;

import java.util.Collections;
import java.util.List;

/**
 * @author rain
 */
@Component
public class MybatisPlusMysqlMapperLocationProvider implements IMapperLocationProvider {

    @Override
    public List<String> getMapperLocations() {
        return Collections.singletonList("classpath*:/vip/isass/framework/database/mybatisplus/mysql/mapper/xml/*Mapper.xml");
    }

}
