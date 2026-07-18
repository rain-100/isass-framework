<#include "./segment/copyright.ftl">

package ${cfg.package}.${cfg.context}.application.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ${cfg.package}.${cfg.context}.domain.criteria.${entity}Criteria;
import ${cfg.package}.${cfg.context}.domain.model.entity.${entity};
import ${cfg.package}.${cfg.context}.infrastructure.persistence.mybatisplus.${entity}Repository;
import vip.isass.framework.nocode.service.ILocalService;

/**
 * <p>
 * <#if table.comment?trim?length gt 0>${table.comment}<#else>${entity}</#if> 本地实现服务
 * </p>
 *
 * @author ${author}
 */
@Slf4j
@Service
public class ${entity}Service implements I${entity}Service, ILocalService<${entity}, ${entity}Criteria> {

    @Autowired
    private ${entity}Repository repository;

    @Override
    public ${entity}Repository getRepository() {
        return repository;
    }

}
