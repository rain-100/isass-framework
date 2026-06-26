<#include "./segment/copyright.ftl">

package ${cfg.mapperPackageName};

import ${cfg.entityPackageName}.V3${entity};
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * <p>
 * <#if table.comment?trim?length gt 0>${table.comment}<#else>${entity}</#if> mapper
 * </p>
 *
 * @author ${author}
 */
public interface V3${table.mapperName} extends BaseMapper<V3${entity}> {

}
