<#include "./segment/copyright.ftl">

<#assign enumStart = "[枚举--">
<#include "./segment/EntityType.ftl">
package ${cfg.entityDbPackageName};

import com.baomidou.mybatisplus.annotation.FieldFill;
<#if isIdEntity>
import com.baomidou.mybatisplus.annotation.IdType;
</#if>
import com.baomidou.mybatisplus.annotation.TableField;
<#if isIdEntity>
import com.baomidou.mybatisplus.annotation.TableId;
</#if>
import com.baomidou.mybatisplus.annotation.TableName;
<#if isVersionEntity>
import com.baomidou.mybatisplus.annotation.Version;
</#if>
import lombok.Getter;
import lombok.Setter;
import ${cfg.entityPackageName}.V2${entity};
import vip.isass.core.structure.entity.IV2DbEntity;

<#list table.fields as field>
<#if field.propertyType == "BigDecimal" && buildInColumns?seq_contains(field.name)>
import java.math.BigDecimal;
<#break>
</#if>
</#list>
<#list table.fields as field>
<#if field.propertyType == "LocalDate" && buildInColumns?seq_contains(field.name)>
import java.time.LocalDate;
<#break>
</#if>
</#list>
<#list table.fields as field>
<#if field.propertyType == "LocalDateTime" && buildInColumns?seq_contains(field.name)>
import java.time.LocalDateTime;
<#break>
</#if>
</#list>
<#list table.fields as field>
<#if field.propertyType == "LocalTime" && buildInColumns?seq_contains(field.name)>
import java.time.LocalTime;
<#break>
</#if>
</#list>

<#------------ BEGIN 定义类名 ------------>
/**
 * <p>
 * <#if table.comment?trim?length gt 0>${table.comment}<#else>${entity}</#if> 数据库实体
 * </p>
 *
 * @author ${author}
 */
@Getter
@Setter
@TableName("${table.name?upper_case}")
public class V2${entity}Db extends V2${entity} implements IV2DbEntity<V2${entity}, V2${entity}Db> {

<#------------ END 定义类名 ------------>
<#list table.fields as field>
<#assign isOrmProperty = false>
<#if field.keyFlag><#assign isOrmProperty = true>
    @TableId(type = IdType.ASSIGN_ID)
</#if>
<#if versionFieldName == field.name><#assign isOrmProperty = true>
    @Version
</#if>
<#if logicDeleteFieldName == field.propertyName><#assign isOrmProperty = true>
    @TableLogic
</#if>
<#if field.name == cfg.traceEntity.CREATE_USER_ID_COLUMN_NAME
    || field.name == cfg.traceEntity.CREATE_USER_NAME_COLUMN_NAME
    || field.name == cfg.traceEntity.CREATED_TIME_COLUMN_NAME><#assign isOrmProperty = true>
    @TableField(fill = FieldFill.INSERT)
</#if>
<#if field.name == cfg.traceEntity.MODIFY_USER_ID_COLUMN_NAME
    || field.name == cfg.traceEntity.MODIFY_USER_NAME_COLUMN_NAME
    || field.name == cfg.traceEntity.MODIFY_TIME_COLUMN_NAME><#assign isOrmProperty = true>
    @TableField(fill = FieldFill.INSERT_UPDATE)
</#if>
<#if isOrmProperty>
    private ${field.propertyType} ${field.propertyName};

</#if>
</#list>
<#---------- END 定义字段 ---------->
}