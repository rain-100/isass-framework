<#include "./segment/copyright.ftl">

<#include "./segment/EntityType.ftl">
package ${cfg.criteriaPackageName};

<#if swagger2>
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
</#if>
import cn.hutool.core.collection.CollUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import ${cfg.entityPackageName}.${entity};
<#if isIdEntity>
import vip.isass.core.criteria.IdCriteria;
<#else>
import vip.isass.core.criteria.AbstractCriteria;
</#if>
<#list table.fields as field>
<#if field.propertyType == "Json">
import vip.isass.core.entity.Json;
<#break>
</#if>
</#list>

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
 * <#if table.comment??>${table.comment!} </#if>查询条件
 *
 * @author ${author}
 */
@Getter
<#if swagger2>@ApiModel("<#if table.comment??>${table.comment}<#else>${entity}</#if>-查询条件")</#if>
public class ${entity}Criteria extends <#if isIdEntity>IdCriteria<${entity}Criteria, ${entity}, ${idEntityPropertyType}><#else>AbstractCriteria<${entity}, ${entity}Criteria></#if> {

<#---------- BEGIN 添加成员变量 ------------>
<#list table.fields as field>
<#if field.name?lower_case == cfg.idEntity.ID_COLUMN_NAME?lower_case>
<#continue>
</#if>
    // region ************************************ ${field.propertyName} ************************************//

<#---------- 所有字段类型都有的查询条件 ------------>
<#include "./segment/criteria_property_name_all_type.java.ftl">
<#---------- String字段类型都有的查询条件 ------------>
<#include "./segment/criteria_property_name_string.java.ftl">
<#---------- 数字字段类型都有的查询条件 ------------>
<#include "./segment/criteria_property_name_number.java.ftl">
<#---------- 集合字段类型都有的查询条件 ------------>
<#include "./segment/criteria_property_name_collection.java.ftl">
</#list>
<#---------- END 添加成员变量 ------------>

<#---------- BEGIN 添加setter方法 ------------>
<#list table.fields as field>
<#if field.name?lower_case == cfg.idEntity.ID_COLUMN_NAME?lower_case>
<#continue>
</#if>
    // endregion ************************************ ${field.propertyName} setter ************************************//

<#---------- 所有字段类型都有的 setter 方法 ------------>
<#include "./segment/criteria_setter_all_type.java.ftl">
<#---------- String字段类型都有的 setter 方法 ------------>
<#include "./segment/criteria_setter_string.java.ftl">
<#---------- 数字字段类型都有的 setter 方法 ------------>
<#include "./segment/criteria_setter_number.java.ftl">
<#---------- 集合字段类型都有的 setter 方法 ------------>
<#include "./segment/criteria_setter_collection.java.ftl">
</#list>
<#---------- END 添加setter方法 ------------>
}