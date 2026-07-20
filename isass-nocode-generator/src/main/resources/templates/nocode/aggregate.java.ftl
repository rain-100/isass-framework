<#include "./segment/copyright.ftl">

package ${cfg.aggregatePackageName};

import ${cfg.entityPackageName}.${entity};

/**
 * <p>
 * <#if table.comment?trim?length gt 0>${table.comment}<#else>${entity}</#if> 聚合。
 * </p>
 *
 * <p>在此添加只涉及本聚合状态与规则的领域行为；不要注入 Repository、Mapper 或 Spring Bean。</p>
 *
 * @author ${author}
 */
public class ${entity}Agg extends ${entity} {

}
