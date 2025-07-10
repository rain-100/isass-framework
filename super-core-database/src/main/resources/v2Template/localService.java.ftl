<#include "./segment/copyright.ftl">

package ${cfg.servicePackageName};

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ${cfg.criteriaPackageName}.V2${entity}Criteria;
import ${cfg.entityPackageName}.V2${entity};
import ${cfg.package}.${cfg.moduleName}.api.service.IV2${entity}Service;
import ${cfg.package}.${cfg.moduleName}.db.repository.V2${entity}Repository;
import vip.isass.core.structure.service.IV2LocalService;

/**
 * <p>
 * <#if table.comment?trim?length gt 0>${table.comment}<#else>${entity}</#if> 本地实现服务
 * </p>
 *
 * @author ${author}
 */
@Slf4j
@Service
public class V2${entity}Service implements IV2${entity}Service, IV2LocalService<V2${entity}, V2${entity}Criteria> {

    @Getter
    @Autowired
    private V2${entity}Service service;

    @Getter
    @Autowired
    private V2${entity}Repository repository;

}
