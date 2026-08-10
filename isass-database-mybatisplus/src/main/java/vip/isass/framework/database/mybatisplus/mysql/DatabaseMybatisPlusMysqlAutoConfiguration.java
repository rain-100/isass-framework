// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.database.mybatisplus.mysql;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import vip.isass.framework.database.mybatisplus.mysql.config.MybatisPlusMysqlMapperLocationProvider;

/**
 * @author Rain
 */
@MapperScan(basePackages = "vip.isass.framework.database.mybatisplus.mysql.mapper")
public class DatabaseMybatisPlusMysqlAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MybatisPlusMysqlMapperLocationProvider mybatisPlusMysqlMapperLocationProvider() {
        return new MybatisPlusMysqlMapperLocationProvider();
    }

}
