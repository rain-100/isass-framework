<#include "./segment/copyright.ftl">

package ${cfg.feignPackage};

import org.springframework.cloud.openfeign.FeignClient;
import ${cfg.criteriaPackageName}.V3${entity}Criteria;
import ${cfg.entityPackageName}.V3${entity};
import ${cfg.package}.${cfg.moduleName}.api.service.IV3${entity}Service;
import vip.isass.core.web.rpc.feign.IV3FeignService;

/**
 * <p>
 * <#if table.comment?trim?length gt 0>${table.comment}<#else>${entity}</#if> feign实现服务
 * </p>
 *
 * @author ${author}
 */
@FeignClient(
        name = "${cfg.controllerPrefix?substring(1)}",
        contextId = "v3${entity}FeignService",
        url = "${r"${feign."}${cfg.moduleName}.url:}",
        primary = false)
public interface V3${entity}FeignService extends
        IV3${entity}Service,
        IV3FeignService<V3${entity}, V3${entity}Criteria> {

}
