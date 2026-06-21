<#include "./segment/copyright.ftl">

package ${cfg.controllerPackageName};

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;
import ${cfg.entityPackageName}.${entity};
import ${cfg.servicePackageName}.${table.serviceName};
import ${cfg.servicePackageName?replace(".service",".${cfg.prefix}.service")}.${cfg.prefix?capFirst}${table.serviceName};
import vip.isass.core.web.IController;

import javax.annotation.Resource;

/**
 * <p>
 * ${table.comment!} controller
 * </p>
 *
 * @author ${author}
 */
@Slf4j
@RestController
public class ${table.controllerName} implements IController<${entity}> {

    @Resource
    private ${cfg.prefix?capFirst}${table.serviceName} ${cfg.prefix}${table.serviceName};

    @Resource
    private ${table.serviceName} ${table.serviceName?uncapFirst};

}
