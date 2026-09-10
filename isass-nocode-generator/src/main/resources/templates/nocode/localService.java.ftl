<#include "./segment/copyright.ftl">
<#include "./segment/EntityType.ftl">

package ${cfg.servicePackageName};

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ${cfg.criteriaPackageName}.${entity}Criteria;
import ${cfg.entityPackageName}.${entity};
import ${cfg.repositoryPackageName}.I${entity}Repository;
import vip.isass.framework.nocode.service.ILocalCrudService;
<#if isParentIdEntity>
import vip.isass.framework.nocode.service.ILocalTreeQueryService;
</#if>

/**
 * <p>
 * <#if tableDescription?trim?length gt 0>${tableDescription}<#else>${entity}</#if> 本地实现服务
 * </p>
 *
 * @author ${author}
 */
@Slf4j
@Service
public class ${entity}Service
        implements I${entity}Service, ILocalCrudService<${entity}, ${entity}Criteria, ${idEntityPropertyType}><#if isParentIdEntity>,
                ILocalTreeQueryService<${entity}, ${entity}Criteria, ${idEntityPropertyType}></#if> {

    @Autowired
    private I${entity}Repository repository;

    @Override
    public I${entity}Repository getRepository() {
        return repository;
    }

    @Override
    public ${entity}Criteria newCriteria() {
        return new ${entity}Criteria();
    }

}
