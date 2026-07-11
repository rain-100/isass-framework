<#include "./segment/copyright.ftl">

package ${cfg.package}.${cfg.moduleName}.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ${cfg.package}.${cfg.moduleName}.api.model.criteria.V3${entity}Criteria;
import ${cfg.package}.${cfg.moduleName}.api.model.entity.V3${entity};
import ${cfg.package}.${cfg.moduleName}.api.service.IV3${entity}Service;
import ${cfg.package}.${cfg.moduleName}.db.repository.V3${entity}Repository;
import vip.isass.framework.nocode.v3.service.IV3LocalService;

/**
 * <p>
 * <#if table.comment?trim?length gt 0>${table.comment}<#else>${entity}</#if> 本地实现服务
 * </p>
 *
 * @author ${author}
 */
@Slf4j
@Service
public class V3${entity}Service implements IV3${entity}Service, IV3LocalService<V3${entity}, V3${entity}Criteria> {

    @Autowired
    private V3${entity}Repository repository;

    @Override
    public V3${entity}Repository getRepository() {
        return repository;
    }

}
