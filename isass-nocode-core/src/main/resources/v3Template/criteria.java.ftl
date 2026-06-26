<#include "./segment/copyright.ftl">

<#include "./segment/EntityType.ftl">
package ${cfg.criteriaPackageName};

import cn.hutool.core.collection.CollUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import ${cfg.entityPackageName}.V3${entity};
import vip.isass.framework.nocode.v3.criteria.IV3Criteria;
<#if isIdEntity>
import vip.isass.framework.nocode.v3.criteria.field.IV3IdCriteria;
</#if>
<#if isParentIdEntity>
import vip.isass.framework.nocode.v3.criteria.field.IV3ParentIdCriteria;
</#if>
<#if isTenantEntity>
import vip.isass.framework.nocode.v3.criteria.field.IV3TenantCriteria;
</#if>
<#if isTraceEntity>
import vip.isass.framework.nocode.v3.criteria.field.IV3TraceCriteria;
</#if>
<#if isVersionEntity>
import vip.isass.framework.nocode.v3.criteria.field.IV3VersionCriteria;
</#if>
import vip.isass.framework.nocode.v3.criteria.impl.type.V3FullTypeCriteria;

import java.beans.Transient;
<#list table.fields as field>
<#if field.propertyType == "BigDecimal">
import java.math.BigDecimal;
<#break>
</#if>
</#list>
<#list table.fields as field>
<#if field.propertyType == "LocalDate">
import java.time.LocalDate;
<#break>
</#if>
</#list>
<#list table.fields as field>
<#if field.propertyType == "LocalDateTime">
import java.time.LocalDateTime;
<#break>
</#if>
</#list>
<#list table.fields as field>
<#if field.propertyType == "LocalTime">
import java.time.LocalTime;
<#break>
</#if>
</#list>
import java.util.Collection;

/**
 * <p>
 * <#if table.comment?trim?length gt 0>${table.comment}<#else>${entity}</#if> 查询条件
 * </p>
 *
 * @author ${author}
 */
public class V3${entity}Criteria
        extends V3FullTypeCriteria<V3${entity}, V3${entity}Criteria>
        implements
<#if isIdEntity>
        IV3IdCriteria<${idEntityPropertyType}, V3${entity}, V3${entity}Criteria>,
</#if>
<#if isParentIdEntity>
        IV3ParentIdCriteria<${parentIdEntityPropertyType}, V3${entity}, V3${entity}Criteria>,
</#if>
<#if isVersionEntity>
        IV3VersionCriteria<V3${entity}, V3${entity}Criteria>,
</#if>
<#if isTenantEntity>
        IV3TenantCriteria<${tenantIdEntityPropertyType}, V3${entity}, V3${entity}Criteria>,
</#if>
<#if isTraceEntity>
        IV3TraceCriteria<String, V3${entity}, V3${entity}Criteria>,
</#if>
        IV3Criteria<V3${entity}, V3${entity}Criteria> {

<#---------- BEGIN 添加 getter setter 方法 ------------>
<#list table.fields as field>
    <#if buildInColumns?seq_contains(field.name?lower_case)><#continue></#if>
    <#if field.propertyType == "JsonNode"><#continue></#if>
    // region ${field.propertyName}

    @Transient
    public ${field.propertyType} get${field.propertyName?cap_first}() {
        return getEquals(V3${entity}.${field.name?upper_case}, ${field.propertyType}.class);
    }

<#---------- 所有字段类型都有的 setter 方法 ------------>
<#include "./segment/criteria_setter_all_type.java.ftl">
<#---------- String字段类型都有的 setter 方法 ------------>
<#include "./segment/criteria_setter_string.java.ftl">
<#---------- 数字字段类型都有的 setter 方法 ------------>
<#include "./segment/criteria_setter_number.java.ftl">
<#---------- 集合字段类型都有的 setter 方法 ------------>
<#include "./segment/criteria_setter_collection.java.ftl">
    // endregion

</#list>

<#---------- END 添加 getter setter 方法 ------------>
}