<#include "./segment/copyright.ftl">

package ${cfg.package}.${cfg.context}.infrastructure.persistence.mybatisplus;

import ${cfg.entityPackageName}.${entity};
import com.github.yulichang.base.MPJBaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * <#if tableDescription?trim?length gt 0>${tableDescription}<#else>${entity}</#if> mapper
 * </p>
 *
 * @author ${author}
 */
@Mapper
public interface ${table.mapperName} extends MPJBaseMapper<${entity}> {

}
