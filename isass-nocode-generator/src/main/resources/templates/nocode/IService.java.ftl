<#include "./segment/copyright.ftl">
<#include "./segment/EntityType.ftl">

package ${cfg.servicePackageName};

import ${cfg.serviceRootPackageName}.ServiceInfo;
import ${cfg.criteriaPackageName}.${entity}Criteria;
import ${cfg.entityPackageName}.${entity};
import vip.isass.framework.entrypoint.annotation.EntrypointInfo;
import vip.isass.framework.nocode.service.ICrudService;
<#if isParentIdEntity>
import vip.isass.framework.nocode.service.ITreeQueryService;
</#if>

/**
 * <p>
 * <#if tableDescription?trim?length gt 0>${tableDescription}<#else>${table.name}</#if> 应用服务接口
 * </p>
 *
 * @author ${author}
 * @tag <#if tableDescription?trim?length gt 0>${tableDescription?replace("\\([^)]*\\)|\\[[^]]*\\]|（[^）]*）|【[^】]*】", "", "r")?trim}<#else>${entity}</#if>
 */
@EntrypointInfo(
        serviceName = ServiceInfo.SERVICE_FULL_NAME,
        contextName = "${cfg.boundedContextName}",
        resourceName = "${entity?uncap_first}",
        tag = ${entity}.COMMENT)
public interface I${entity}Service
        extends ICrudService<${entity}, ${entity}Criteria, ${idEntityPropertyType}><#if isParentIdEntity>,
                ITreeQueryService<${entity}, ${entity}Criteria, ${idEntityPropertyType}></#if> {

    // region 新业务方法
    // 自定义远程方法必须声明 @EntrypointOperation 和参数来源注解。

    // endregion

}
