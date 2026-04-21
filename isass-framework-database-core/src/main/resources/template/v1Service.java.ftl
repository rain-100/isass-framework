<#include "./segment/copyright.ftl">

package ${cfg.servicePackageName?replace(".service",".${cfg.prefix}.service")};

import lombok.Getter;
import org.springframework.stereotype.Service;
import vip.isass.core.web.IV1Service;
import ${cfg.criteriaPackageName}.${entity}Criteria;
import ${cfg.entityPackageName}.${entity};
import ${cfg.package}.${cfg.moduleName}.db.${cfg.prefix}.repository.${cfg.prefix?cap_first}${entity}Repository;

import javax.annotation.Resource;

/**
 * <p>
 * <#if table.comment??>${table.comment!} </#if>服务
 * </p>
 *
 * @author ${author}
 */
@Service
public class ${cfg.prefix?cap_first}${table.serviceName} implements IV1Service<${entity}, ${entity}Criteria> {

    @Getter
    @Resource
    private ${cfg.prefix?cap_first}${entity}Repository v1Repository;

}
