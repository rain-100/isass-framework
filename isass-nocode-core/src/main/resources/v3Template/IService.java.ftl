<#include "./segment/copyright.ftl">

package ${cfg.package}.${cfg.moduleName}.api.service;

import ${cfg.package}.${cfg.moduleName}.api.ModuleInfo;
import ${cfg.criteriaPackageName}.V3${entity}Criteria;
import ${cfg.entityPackageName}.V3${entity};
import vip.isass.framework.nocode.v3.service.IV3Service;

/**
 * <p>
 * <#if table.comment??>${table.comment}<#else>${table.name}</#if> 服务接口
 * </p>
 *
 * @author ${author}
 */
public interface IV3${entity}Service extends IV3Service<V3${entity}, V3${entity}Criteria> {

    /** 当前实体相对于微服务 URL 前缀的 V3 路由。 */
    String URI_SECOND_PART = "/${entity?uncap_first}/v3";

    /** 当前实体的完整 V3 HTTP 路由。 */
    String URI_FIRST_PART = ModuleInfo.SERVICE_URL_PREFIX + URI_SECOND_PART;

    // region 新业务方法

    // endregion

}
