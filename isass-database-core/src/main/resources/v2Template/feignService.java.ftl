<#include "./segment/copyright.ftl">

package ${cfg.feignPackage};

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import ${cfg.criteriaPackageName}.V2${entity}Criteria;
import ${cfg.entityPackageName}.V2${entity};
import ${cfg.package}.${cfg.moduleName}.api.service.${table.serviceName};
import vip.isass.framework.rpc.feign.IV2FeignService;

/**
 * <p>
 * <#if table.comment?trim?length gt 0>${table.comment}<#else>${entity}</#if> feign实现服务
 * </p>
 *
 * @author ${author}
 */
@FeignClient(
        name = "${cfg.controllerPrefix?substring(1)}",
        contextId = "v2${entity}FeignService",
        url = "${r"${feign."}${cfg.moduleName}.url:}",
        primary = false)
@RequestMapping(${table.serviceName}.URI_FIRST_PART)
public interface V2${entity}FeignService extends
        ${table.serviceName},
        IV2FeignService<V2${entity}, V2${entity}Criteria> {

}
