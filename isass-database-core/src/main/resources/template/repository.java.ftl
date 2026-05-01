<#include "./segment/copyright.ftl">

package ${cfg.package}.${cfg.moduleName}.db.repository;

import org.springframework.stereotype.Repository;
import ${cfg.criteriaPackageName}.${entity}Criteria;
import ${cfg.entityPackageName}.${entity};
import ${cfg.mapperPackageName}.${entity}Mapper;
import ${cfg.entityDbPackageName}.${entity}Db;
import vip.isass.core.database.mybatisplus.plus.MybatisPlusRepository;

/**
 * <p>
 * <#if table.comment??>${table.comment!} </#if>数据仓库
 * </p>
 *
 * @author ${author}
 */
@Repository
public class ${entity}Repository extends MybatisPlusRepository<${entity}, ${entity}Db, ${entity}Criteria, ${entity}Mapper> {

}
