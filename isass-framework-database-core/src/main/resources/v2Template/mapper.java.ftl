<#include "./segment/copyright.ftl">

package ${cfg.mapperPackageName};

import ${cfg.entityDbPackageName}.V2${entity}Db;
import vip.isass.core.database.mybatisplus.mapper.IMapper;

/**
 * <p>
 * <#if table.comment?trim?length gt 0>${table.comment}<#else>${entity}</#if> mapper
 * </p>
 *
 * @author ${author}
 */
public interface V2${table.mapperName} extends IMapper<V2${entity}Db> {

}
