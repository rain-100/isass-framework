<#include "./segment/copyright.ftl">

package ${cfg.package}.${cfg.moduleName}.controller;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ${cfg.package}.${cfg.moduleName}.api.ModuleInfo;
import ${cfg.package}.${cfg.moduleName}.service.${entity}Service;

/**
 * <p>
 * <#if table.comment?trim?length gt 0>${table.comment}<#else>${entity}</#if> 手写扩展接口
 * </p>
 *
 * @apiNote 业务模块自定义接口。
 * @author ${author}
 * @tag <#if table.comment?trim?length gt 0>${table.comment}<#else>${entity}</#if>
 */
@Slf4j
@RestController
@RequestMapping(ModuleInfo.SERVICE_URL_PREFIX)
public class ${entity}Controller {

    @Resource
    private ${entity}Service nocode${entity}Service;

}
