<#include "./segment/copyright.ftl">

package ${cfg.mapperPackageName};

import ${cfg.entityDbPackageName}.${entity}Db;
import mapper.vip.isass.framework.database.mybatisplus.IMapper;

/**
 * <p>
 * <#if table.comment??>${table.comment!} </#if>Mapper
 * </p>
 *
 * @author ${author}
 */
public interface ${table.mapperName} extends IMapper<${entity}Db> {

}
