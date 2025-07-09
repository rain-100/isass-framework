<#include "./segment/copyright.ftl">

package ${cfg.package}.${cfg.moduleName}.db.repository;

import org.springframework.stereotype.Repository;
import ${cfg.criteriaPackageName}.V2${entity}Criteria;
import ${cfg.entityPackageName}.V2${entity};
import ${cfg.mapperPackageName}.V2${entity}Mapper;
import ${cfg.entityDbPackageName}.V2${entity}Db;
import vip.isass.core.database.mybatisplus.plus.V2MybatisPlusRepository;

/**
 * <p>
 * <#if table.comment?trim?length gt 0>${table.comment}<#else>${entity}</#if> 数据仓库
 * </p>
 *
 * @author ${author}
 */
@Repository
public class V2${entity}Repository extends V2MybatisPlusRepository<
        V2${entity},
        V2${entity}Db,
        V2${entity}Criteria,
        V2${entity}Mapper> {

}
