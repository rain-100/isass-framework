<#include "./segment/copyright.ftl">

package ${cfg.nocodeRepositoryPackageName};

import org.springframework.stereotype.Repository;
import vip.isass.framework.nocode.v3.orm.V3MybatisPlusRepository;
import ${cfg.nocodeCriteriaPackageName}.V3${entity}Criteria;
import ${cfg.nocodeEntityPackageName}.V3${entity};
import ${cfg.nocodeMapperPackageName}.V3${entity}Mapper;

/**
 * <p>
 * <#if table.comment?trim?length gt 0>${table.comment}<#else>${entity}</#if> 数据仓库
 * </p>
 *
 * @author ${author}
 */
@Repository
public class V3${entity}Repository extends V3MybatisPlusRepository<
        V3${entity},
        V3${entity}Criteria,
        V3${entity}Mapper> {

}
