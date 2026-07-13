<#include "./segment/copyright.ftl">

package ${cfg.package}.${cfg.moduleName}.application.service;

import ${cfg.package}.${cfg.moduleName}.ModuleInfo;
import ${cfg.criteriaPackageName}.${entity}Criteria;
import ${cfg.entityPackageName}.${entity};
import vip.isass.framework.nocode.service.IService;

/**
 * <p>
 * <#if table.comment??>${table.comment}<#else>${table.name}</#if> 服务接口
 * </p>
 *
 * @author ${author}
 * @tag <#if table.comment?trim?length gt 0>${table.comment}<#else>${entity}</#if>
 */
public interface I${entity}Service extends IService<${entity}, ${entity}Criteria> {

    /** 当前实体相对于微服务 URL 前缀的  路由。 */
    String URI_SECOND_PART = "/${entity?uncap_first}";

    /** 当前实体的完整  HTTP 路由。 */
    String URI_FIRST_PART = ModuleInfo.SERVICE_URL_PREFIX + URI_SECOND_PART;

    // region 新业务方法

    // endregion

}
