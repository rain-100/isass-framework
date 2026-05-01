<#include "./segment/copyright.ftl">

package ${cfg.mapperPackageName?replace(".mapper",".${cfg.prefix}.mapper")};

import ${cfg.entityDbPackageName}.${entity}Db;
import vip.isass.core.database.mybatisplus.mapper.IMapper;

/**
 * <p>
 * <#if table.comment??>${table.comment!} </#if>Mapper
 * </p>
 *
 * @author ${author}
 */
public interface ${cfg.prefix?cap_first}${table.mapperName} extends IMapper<${entity}Db> {

}
