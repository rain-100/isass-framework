<#include "./segment/copyright.ftl">

package ${cfg.package}.${cfg.moduleName}.db.mapper;

import ${cfg.package}.${cfg.moduleName}.api.model.entity.${entity};
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * <p>
 * <#if table.comment?trim?length gt 0>${table.comment}<#else>${entity}</#if> mapper
 * </p>
 *
 * @author ${author}
 */
public interface ${table.mapperName} extends BaseMapper<${entity}> {

}
