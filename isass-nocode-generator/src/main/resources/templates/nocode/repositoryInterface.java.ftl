<#include "./segment/copyright.ftl">

package ${cfg.repositoryPackageName};

import ${cfg.criteriaPackageName}.${entity}Criteria;
import ${cfg.entityPackageName}.${entity};
import vip.isass.framework.nocode.repository.IRepository;

/**
 * <p>
 * <#if table.comment?trim?length gt 0>${table.comment}<#else>${entity}</#if> 领域数据仓库。
 * </p>
 *
 * @author ${author}
 */
public interface I${entity}Repository extends IRepository<${entity}, ${entity}Criteria> {

}
