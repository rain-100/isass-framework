<#include "./segment/copyright.ftl">

package ${cfg.nocodeLocalServicePackageName};

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ${cfg.nocodeCriteriaPackageName}.V3${entity}Criteria;
import ${cfg.nocodeEntityPackageName}.V3${entity};
import ${cfg.nocodeServicePackageName}.IV3${entity}Service;
import ${cfg.nocodeRepositoryPackageName}.V3${entity}Repository;
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

    @Getter
    @Autowired
    private V3${entity}Repository repository;

}
