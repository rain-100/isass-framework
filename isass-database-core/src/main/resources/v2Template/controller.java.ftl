<#include "./segment/copyright.ftl">

package ${cfg.controllerPackageName};

import io.swagger.annotations.Api;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ${cfg.criteriaPackageName}.V2${entity}Criteria;
import ${cfg.entityPackageName}.V2${entity};
import ${cfg.package}.${cfg.moduleName}.api.service.${table.serviceName};
import ${cfg.servicePackageName}.V2${table.serviceImplName};
import vip.isass.framework.web.structure.IV2Controller;

/**
 * <p>
 * <#if table.comment?trim?length gt 0>${table.comment}<#else>${entity}</#if> controller
 * </p>
 *
 * @author ${author}
 */
@Slf4j
@Api(tags = "v2<#if table.comment?trim?length gt 0>${table.comment}<#else>${entity}</#if>")
@RestController
@RequestMapping(${table.serviceName}.URI_FIRST_PART)
public class ${table.controllerName} implements ${table.serviceName}, IV2Controller<V2${entity}, V2${entity}Criteria> {

    @Getter
    @Autowired
    private V2${table.serviceImplName} service;

}
