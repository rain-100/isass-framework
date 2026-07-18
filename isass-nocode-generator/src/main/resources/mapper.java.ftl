<#include "./segment/copyright.ftl">

package ${cfg.package}.${cfg.context}.infrastructure.persistence.mybatisplus;

import ${cfg.package}.${cfg.context}.domain.model.entity.${entity};
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * <#if table.comment?trim?length gt 0>${table.comment}<#else>${entity}</#if> mapper
 * </p>
 *
 * @author ${author}
 */
@Mapper
public interface ${table.mapperName} extends BaseMapper<${entity}> {

}
