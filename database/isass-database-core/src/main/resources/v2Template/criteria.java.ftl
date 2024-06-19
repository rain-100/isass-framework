<#include "./segment/copyright.ftl">

<#include "./segment/EntityType.ftl">
package ${cfg.criteriaPackageName};

import cn.hutool.core.collection.CollUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModelProperty;
import ${cfg.entityPackageName}.V2${entity};
import vip.isass.core.structure.criteria.IV2Criteria;
<#if isIdEntity>
import vip.isass.core.structure.criteria.field.IV2IdCriteria;
</#if>
<#if isParentIdEntity>
import vip.isass.core.structure.criteria.field.IV2ParentIdCriteria;
</#if>
<#if isTenantEntity>
import vip.isass.core.structure.criteria.field.IV2TenantCriteria;
</#if>
<#if isTraceEntity>
import vip.isass.core.structure.criteria.field.IV2TraceCriteria;
</#if>
<#if isVersionEntity>
import vip.isass.core.structure.criteria.field.IV2VersionCriteria;
</#if>
import vip.isass.core.structure.criteria.impl.type.V2FullTypeCriteria;

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
public class V2${entity}Criteria
        extends V2FullTypeCriteria<V2${entity}, V2${entity}Criteria>
        implements
<#if isIdEntity>
        IV2IdCriteria<${idEntityPropertyType}, V2${entity}, V2${entity}Criteria>,
</#if>
<#if isParentIdEntity>
        IV2ParentIdCriteria<${parentIdEntityPropertyType}, V2${entity}, V2${entity}Criteria>,
</#if>
<#if isVersionEntity>
        IV2VersionCriteria<V2${entity}, V2${entity}Criteria>,
</#if>
<#if isTenantEntity>
        IV2TenantCriteria<${tenantIdEntityPropertyType}, V2${entity}, V2${entity}Criteria>,
</#if>
<#if isTraceEntity>
        IV2TraceCriteria<String, V2${entity}, V2${entity}Criteria>,
</#if>
        IV2Criteria<V2${entity}, V2${entity}Criteria> {

<#---------- BEGIN 添加 getter setter 方法 ------------>
<#list table.fields as field>
    <#if buildInColumns?seq_contains(field.name)><#continue></#if>
    <#if field.propertyType == "Json"><#continue></#if>
    // region ${field.propertyName}

    @Transient
    @ApiModelProperty("<#if (field.comment?trim?length > 0)>${field.comment}<#else>${field.propertyName}</#if>")
    public ${field.propertyType} get${field.propertyName?cap_first}() {
        return getEquals(V2${entity}.${field.name?upper_case}, ${field.propertyType}.class);
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