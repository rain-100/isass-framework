<#include "./segment/copyright.ftl">

package ${cfg.mapperPackageName};

import ${cfg.entityDbPackageName}.${entity}Db;
import vip.isass.core.database.mybatisplus.mapper.IMapper;

/**
 * <p>
 * <#if table.comment??>${table.comment!} </#if>Mapper
 * </p>
 *
 * @author ${author}
 */
public interface ${table.mapperName} extends IMapper<${entity}Db> {

}
