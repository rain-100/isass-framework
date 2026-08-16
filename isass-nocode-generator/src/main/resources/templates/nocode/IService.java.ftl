<#include "./segment/copyright.ftl">
<#include "./segment/EntityType.ftl">

package ${cfg.package}.${cfg.context}.application.service;

import ${cfg.serviceInfoPackageName}.ServiceInfo;
import ${cfg.criteriaPackageName}.${entity}Criteria;
import ${cfg.entityPackageName}.${entity};
import vip.isass.framework.entrypoint.annotation.EntrypointInfo;
import vip.isass.framework.nocode.service.ICrudService;

/**
 * <p>
 * <#if table.comment??>${table.comment}<#else>${table.name}</#if> 应用服务接口
 * </p>
 *
 * @author ${author}
 * @tag <#if table.comment?trim?length gt 0>${table.comment?replace("\\([^)]*\\)|\\[[^]]*\\]|（[^）]*）|【[^】]*】", "", "r")?trim}<#else>${entity}</#if>
 */
@EntrypointInfo(
        serviceName = ServiceInfo.SERVICE_FULL_NAME,
        contextName = "${cfg.boundedContextName}",
        resourceName = "${entity?uncap_first}")
public interface I${entity}Service
        extends ICrudService<${entity}, ${entity}Criteria, ${idEntityPropertyType}> {

    // region 新业务方法
    // 自定义远程方法必须声明 @EntrypointOperation 和参数来源注解。

    // endregion

}
