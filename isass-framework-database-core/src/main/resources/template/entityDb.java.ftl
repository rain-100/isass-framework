<#include "./segment/copyright.ftl">

<#assign enumStart = "[枚举--">
<#include "./segment/EntityType.ftl">
package ${cfg.entityDbPackageName};

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ${cfg.entityPackageName}.${entity};
import vip.isass.core.entity.DbEntity;

<#list table.fields as field>
<#include "./segment/isBuildinProperty.ftl">
<#if field.propertyType == "BigDecimal" && isBuildinProperty>
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
<#list table.fields as field>
<#if field.propertyType?starts_with("Collection")>
import java.util.Collection;
<#break>
</#if>
</#list>

<#------------ BEGIN 定义类名 ------------>
/**
 * <p>
 * ${table.comment!}
 * </p>
 *
 * @author ${author}
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("${table.name}")
public class ${entity}Db extends ${entity} implements DbEntity<${entity}, ${entity}Db> {

<#------------ END 定义类名 ------------>
<#list table.fields as field>
<#assign isOrmProperty = false>
    <#if field.keyFlag><#assign isOrmProperty = true>
    @TableId(type = IdType.ASSIGN_ID)
    </#if>
    <#if versionFieldName == field.name><#assign isOrmProperty = true>
    @Version
    </#if>
    <#if logicDeleteFieldName == field.name><#assign isOrmProperty = true>
    @TableLogic
    </#if>
    <#if field.propertyName == cfg.userTracedEntity.CREATE_USER_ID_PROPERTY
    || field.propertyName == cfg.userTracedEntity.CREATE_USER_NAME_PROPERTY
    || field.propertyName == cfg.timeTracedEntity.CREATED_TIME_PROPERTY><#assign isOrmProperty = true>
    @TableField(fill = FieldFill.INSERT)
    </#if>
    <#if field.propertyName == cfg.userTracedEntity.MODIFY_USER_ID_PROPERTY
    || field.propertyName == cfg.userTracedEntity.MODIFY_USER_NAME_PROPERTY
    || field.propertyName == cfg.timeTracedEntity.MODIFY_TIME_PROPERTY><#assign isOrmProperty = true>
    @TableField(fill = FieldFill.INSERT_UPDATE)
    </#if><#if isOrmProperty>
    private <#if field.comment!?contains("${enumStart}")>${field.propertyName?cap_first} <#else>${field.propertyType} </#if>${field.propertyName};

</#if>
</#list>
<#---------- END 定义字段 ---------->
}