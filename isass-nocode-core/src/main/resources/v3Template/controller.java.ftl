<#include "./segment/copyright.ftl">

package ${cfg.package}.${cfg.moduleName}.controller;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ${cfg.nocodeCriteriaPackageName}.V3${entity}Criteria;
import ${cfg.nocodeEntityPackageName}.V3${entity};
import ${cfg.nocodeLocalServicePackageName}.V3${entity}Service;
import vip.isass.framework.nocode.v3.controller.IV3Controller;

/**
 * <#if table.comment?trim?length gt 0>${table.comment}<#else>${entity}</#if> V3 Controller
 *
 * @author ${author}
 * @tag <#if table.comment?trim?length gt 0>${table.comment}<#else>${entity}</#if>
 */
@RestController
@RequestMapping("${cfg.controllerPrefix}/${entity?uncap_first}")
public class V3${entity}Controller implements IV3Controller<V3${entity}, V3${entity}Criteria> {

    @Getter
    @Autowired
    private V3${entity}Service service;

}
