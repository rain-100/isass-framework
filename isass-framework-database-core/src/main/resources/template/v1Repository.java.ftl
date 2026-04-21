<#include "./segment/copyright.ftl">

package ${cfg.package}.${cfg.moduleName}.db.${cfg.prefix}.repository;

import org.springframework.stereotype.Repository;
import ${cfg.criteriaPackageName}.${entity}Criteria;
import ${cfg.entityPackageName}.${entity};
import ${cfg.entityDbPackageName}.${entity}Db;
import ${cfg.package}.${cfg.moduleName}.db.${cfg.prefix}.mapper.${cfg.prefix?cap_first}${entity}Mapper;
import vip.isass.core.database.mybatisplus.plus.MybatisPlusRepository;

/**
 * <p>
 * <#if table.comment??>${table.comment!} </#if>数据仓库
 * </p>
 *
 * @author ${author}
 */
@Repository
public class ${cfg.prefix?cap_first}${entity}Repository extends MybatisPlusRepository<${entity}, ${entity}Db, ${entity}Criteria, ${cfg.prefix?cap_first}${entity}Mapper> {

}
