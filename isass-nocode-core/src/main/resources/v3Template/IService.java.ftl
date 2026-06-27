<#include "./segment/copyright.ftl">

package ${cfg.package}.${cfg.moduleName}.api.service;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import ${cfg.criteriaPackageName}.V3${entity}Criteria;
import ${cfg.entityPackageName}.V3${entity};
import vip.isass.framework.nocode.v3.service.IV3Service;
import vip.isass.framework.nocode.v3.service.IV3ServiceManager;

import java.util.List;

/**
 * <p>
 * <#if table.comment??>${table.comment}<#else>${table.name}</#if> 服务接口
 * </p>
 *
 * @author ${author}
 */
public interface IV3${entity}Service extends IV3Service<V3${entity}, V3${entity}Criteria> {

    // region 新业务方法

    // endregion

    @Primary
    @Service
    static class V3${entity}ServiceManager implements
            IV3${entity}Service,
            IV3ServiceManager<V3${entity}, V3${entity}Criteria, IV3${entity}Service> {

        @Getter
        @Autowired(required = false)
        private List<IV3${entity}Service> services;

        // region 新业务方法

        // endregion

    }

}