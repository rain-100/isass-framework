<#include "./segment/copyright.ftl">

<#include "./segment/EntityType.ftl">
package ${cfg.criteriaPackageName};

import cn.hutool.core.collection.CollUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import ${cfg.entityPackageName}.${entity};
import vip.isass.framework.nocode.criteria.ICriteria;
<#if isIdEntity>
import vip.isass.framework.nocode.criteria.field.IIdCriteria;
</#if>
<#if isParentIdEntity>
import vip.isass.framework.nocode.criteria.field.IParentIdCriteria;
</#if>
<#if isTenantEntity>
import vip.isass.framework.nocode.criteria.field.ITenantCriteria;
</#if>
<#if isTraceEntity>
import vip.isass.framework.nocode.criteria.field.ITraceCriteria;
</#if>
<#if isVersionEntity>
import vip.isass.framework.nocode.criteria.field.IVersionCriteria;
</#if>
import vip.isass.framework.nocode.criteria.impl.type.FullTypeCriteria;

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
public class ${entity}Criteria
        extends FullTypeCriteria<${entity}, ${entity}Criteria>
        implements
<#if isIdEntity>
        IIdCriteria<${idEntityPropertyType}, ${entity}, ${entity}Criteria>,
</#if>
<#if isParentIdEntity>
        IParentIdCriteria<${parentIdEntityPropertyType}, ${entity}, ${entity}Criteria>,
</#if>
<#if isVersionEntity>
        IVersionCriteria<${entity}, ${entity}Criteria>,
</#if>
<#if isTenantEntity>
        ITenantCriteria<${tenantIdEntityPropertyType}, ${entity}, ${entity}Criteria>,
</#if>
<#if isTraceEntity>
        ITraceCriteria<${idEntityPropertyType}, ${entity}, ${entity}Criteria>,
</#if>
        ICriteria<${entity}, ${entity}Criteria> {

<#---------- BEGIN 添加 getter setter 方法 ------------>
<#list table.fields as field>
    <#if buildInColumns?seq_contains(field.name?lower_case)
        && !(field.name?lower_case == cfg.tenantEntity.TENANT_ID_COLUMN_NAME && field.comment!?contains("[tenantEntity--false]"))><#continue></#if>
    <#if field.propertyType == "JsonNode"><#continue></#if>
    // region ${field.propertyName}

    @Transient
    public ${field.propertyType} get${field.propertyName?cap_first}() {
        return getEquals(${entity}.${field.name?upper_case}, ${field.propertyType}.class);
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
