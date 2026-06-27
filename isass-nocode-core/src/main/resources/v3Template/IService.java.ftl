<#include "./segment/copyright.ftl">

package ${cfg.package}.${cfg.moduleName}.api.service;

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

    // region 新业务方法

    // endregion

}
