<#include "./segment/copyright.ftl">

package ${cfg.package}.${cfg.context}.infrastructure.persistence.mybatisplus;

import org.springframework.stereotype.Repository;
import vip.isass.framework.nocode.orm.MybatisPlusRepository;
import ${cfg.criteriaPackageName}.${entity}Criteria;
import ${cfg.entityPackageName}.${entity};
import ${cfg.repositoryPackageName}.I${entity}Repository;

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
        ${entity}Mapper> implements I${entity}Repository {

}
