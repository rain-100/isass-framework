<#include "./segment/copyright.ftl">

package ${cfg.package}.${cfg.moduleName}.db.repository;

import org.springframework.stereotype.Repository;
import vip.isass.framework.nocode.orm.MybatisPlusRepository;
import ${cfg.package}.${cfg.moduleName}.api.model.criteria.${entity}Criteria;
import ${cfg.package}.${cfg.moduleName}.api.model.entity.${entity};
import ${cfg.package}.${cfg.moduleName}.db.mapper.${entity}Mapper;

/**
 * <p>
 * <#if table.comment?trim?length gt 0>${table.comment}<#else>${entity}</#if> 数据仓库
 * </p>
 *
 * @author ${author}
 */
@Repository
public class ${entity}Repository extends MybatisPlusRepository<
        ${entity},
        ${entity}Criteria,
        ${entity}Mapper> {

}
