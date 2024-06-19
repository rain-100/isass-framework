<#include "./segment/copyright.ftl">
<#include "./segment/EntityType.ftl">

package ${cfg.controllerPackageName?replace(".controller",".${cfg.prefix}.controller")};

import io.swagger.annotations.Api;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vip.isass.core.web.IV1Controller;
import ${cfg.criteriaPackageName}.${entity}Criteria;
import ${cfg.entityPackageName}.${entity};
import ${cfg.servicePackageName?replace(".service",".${cfg.prefix}.service")}.${cfg.prefix?cap_first}${table.serviceName};

import javax.annotation.Resource;

/**
 * <p>
 * <#if table.comment??>${table.comment!} </#if>控制器
 * </p>
 *
 * @author ${author}
 */
@Slf4j
@RestController
@Api(tags = "v1<#if table.comment??>${table.comment!}<#else>${table.name?replace("${cfg.tablePrefix[0]}_","","f")?replace("_","-")}</#if>")
@RequestMapping("${cfg.controllerPrefix}/v1/${table.name?replace("${cfg.tablePrefix[0]}_","","f")?replace("_","-")}")
public class ${cfg.prefix?cap_first}${table.controllerName} implements IV1Controller<${entity}, ${entity}Criteria, V1${table.serviceName}> {

    @Getter
    @Resource
    private ${cfg.prefix?cap_first}${table.serviceName} service;

}
