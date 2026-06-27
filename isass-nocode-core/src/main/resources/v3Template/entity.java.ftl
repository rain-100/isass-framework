<#include "./segment/copyright.ftl">

<#assign enumStart = "[枚举--">
<#include "./segment/EntityType.ftl">
package ${cfg.entityPackageName};

<#list table.fields as field>
<#if field.comment!?contains("${enumStart}")>
import cn.hutool.core.util.RandomUtil;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
</#if>
</#list>
<#list table.fields as field>
<#if field.propertyType == "JsonNode">
import com.fasterxml.jackson.databind.JsonNode;
</#if>
</#list>
<#list table.fields as field>
<#if field.propertyName!?ends_with("Id") && field.propertyType == "Long">
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
<#break>
</#if>
</#list>
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import vip.isass.framework.nocode.v3.entity.IV3Entity;
<#if isIdEntity>
import vip.isass.framework.nocode.v3.entity.IV3IdEntity;
</#if>
<#if isLogicDeleteEntity>
import vip.isass.framework.nocode.v3.entity.IV3LogicDeleteEntity;
</#if>
<#if isParentIdEntity>
import vip.isass.framework.nocode.v3.entity.IV3ParentIdEntity;
</#if>
<#if isTenantEntity>
import vip.isass.framework.nocode.v3.entity.IV3TenantEntity;
</#if>
<#if isTraceEntity>
import vip.isass.framework.nocode.v3.entity.IV3TraceEntity;
</#if>
<#if isVersionEntity>
import vip.isass.framework.nocode.v3.entity.IV3VersionEntity;
</#if>
<#list table.fields as field>
<#if (field.propertyType == "LocalDate"
|| field.propertyType == "LocalTime"
|| field.propertyType == "LocalDateTime")
&& field.name?lower_case != cfg.traceEntity.CREATED_TIME_COLUMN_NAME
&& field.name?lower_case != cfg.traceEntity.MODIFY_TIME_COLUMN_NAME>
import vip.isass.core.support.LocalDateTimeUtil;
<#break>
</#if>
</#list>

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
<#list table.fields as field>
<#if field.propertyType?starts_with("Collection")>
import java.util.Collection;
<#break>
</#if>
</#list>

<#------------ BEGIN 定义类名 ------------>
/**
 * <p>
 * <#if table.comment?trim?length gt 0>${table.comment}<#else>${entity}</#if> 实体
 * </p>
 *
 * @author ${author}
 */
@Getter
@Setter
@ToString
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class V3${entity} implements
<#if isIdEntity>
        IV3IdEntity<${idEntityPropertyType}, V3${entity}>,
</#if>
<#if isParentIdEntity>
        IV3ParentIdEntity<${parentIdEntityPropertyType}, V3${entity}>,
</#if>
<#if isVersionEntity>
        IV3VersionEntity<V3${entity}>,
</#if>
<#if isTenantEntity>
        IV3TenantEntity<${tenantIdEntityPropertyType}, V3${entity}>,
</#if>
<#if isLogicDeleteEntity>
        IV3LogicDeleteEntity<V3${entity}>,
</#if>
<#if isTraceEntity>
        IV3TraceEntity<String, V3${entity}>,
</#if>
        IV3Entity<V3${entity}> {

<#------------ END 定义类名 ------------>
<#------------ BEGIN 定义公共字段 ------------>
    public static final V3${entity} EMPTY = new V3${entity}();

    private static final long serialVersionUID = 1L;

<#------------ END 定义公共字段 ------------>
<#------------ BEGIN 定义数据库字段名 ------------>
<#list table.fields as field>
    public transient static final String ${field.name?upper_case} = "${field.propertyName}";
    public transient static final String ${field.name?upper_case}_COLUMN_NAME = "${field.name}";

</#list>
<#---------- BEGIN 定义字段 ------------>
<#list table.fields as field>
    /**
     * <p>
     * <#if (field.comment?trim?length > 0)>${field.comment}<#else>${field.propertyName}</#if>
     * </p>
     * 数据库字段名: ${field.name}
     * 数据库字段类型: ${field.metaInfo.typeName}
     */
<#if field.propertyName!?ends_with("Id") && field.propertyType == "Long">
    @JsonSerialize(using = ToStringSerializer.class)</#if>
    private <#if field.comment!?contains("${enumStart}")>${field.propertyName?cap_first}<#elseif field.propertyType == "JsonNode">JsonNode<#else>${field.propertyType}</#if> ${field.propertyName};

</#list>
<#---------- END 定义字段 ---------->
<#---------- START 添加枚举类 ---------->
<#list table.fields as field>
    <#if field.comment!?contains("${enumStart}")>
        <#assign start = field.comment?index_of("${enumStart}") + enumStart?length>
        <#assign end = field.comment?index_of("]", start)>
        <#assign enumStringArr = field.comment?substring(start, end)?split(";")>
    public enum ${field.propertyName?cap_first} {

<#list enumStringArr as enumString>
<#assign enumArr = enumString?split(":")>
        ${enumArr[1]}(${enumArr[0]}, "${enumArr[2]}")<#if (enumString_index + 1) == enumStringArr?size>;<#else>,</#if>
        </#list>

        private final Integer code;

        @Getter
        private final String desc;

        ${field.propertyName?cap_first}(Integer code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        @JsonValue
        public Integer getCode() {
            return code;
        }

        @JsonCreator
        public static ${field.propertyName?cap_first} parseFromCode(Integer code) {
            for (${field.propertyName?cap_first} ${field.propertyName} : ${field.propertyName?cap_first}.values()) {
                if (${field.propertyName}.code.equals(code)) {
                    return ${field.propertyName};
                }
            }
            return null;
        }

        public static ${field.propertyName?cap_first} parseFromCodeOrException(Integer code) {
            ${field.propertyName?cap_first} ${field.propertyName} = parseFromCode(code);
            if (${field.propertyName} == null) {
                throw new IllegalArgumentException("不支持的参数：${field.propertyName?cap_first}.code: " + code);
            }
            return ${field.propertyName};
        }

        public static ${field.propertyName?cap_first} random() {
            return values()[RandomUtil.randomInt(${field.propertyName?cap_first}.values().length)];
        }

    }

    </#if>
</#list>
<#---------- END 添加枚举类 ---------->
<#---------- START 添加IdEntity的方法 ---------->
<#if isIdEntity>
<#-- 当主键字段名与默认主键字段名不一致时，添加默认主键字段名的get、set方法 -->
<#if idEntityColumnName != cfg.idEntity.ID_COLUMN_NAME>
    @Override
    public ${idEntityPropertyType} getId() {
        return this.${idEntityPropertyName};
    }

    @Override
    public void setId(${idEntityPropertyType} id) {
        this.${idEntityPropertyName} = id;
    }

</#if>
</#if>
<#---------- END 添加IdEntity的方法 ---------->
<#---------- START 添加Entity的randomEntity方法 ---------->
    @Override
    public V3${entity} randomEntity() {
<#if isIdEntity>
        IV3IdEntity.super.randomEntity();
</#if>
<#if isParentIdEntity>
        IV3ParentIdEntity.super.randomEntity();
</#if>
<#if isVersionEntity>
        IV3VersionEntity.super.randomEntity();
</#if>
<#if isTenantEntity>
        IV3TenantEntity.super.randomEntity();
</#if>
<#if isLogicDeleteEntity>
        IV3LogicDeleteEntity.super.randomEntity();
</#if>
<#if isTraceEntity>
        IV3TraceEntity.super.randomEntity();
</#if>
<#list table.fields as field>
    <#if field.keyFlag><#continue></#if>
    <#if field.propertyType?ends_with("[]")><#continue></#if>
    <#if field.propertyType == "JsonNode"><#continue></#if>
    <#if field.propertyType == "Collection<String>"><#continue></#if>
    <#if buildInColumns?seq_contains(field.name?lower_case)><#continue></#if>
    <#if field.propertyType == "LocalTime">
        set${field.propertyName?cap_first}(LocalDateTimeUtil.nowLocalTime());
        <#continue>
    </#if>
    <#if field.propertyType == "LocalDate">
        set${field.propertyName?cap_first}(LocalDateTimeUtil.nowLocalDate());
        <#continue>
    </#if>
    <#if field.propertyType == "LocalDateTime">
        set${field.propertyName?cap_first}(LocalDateTimeUtil.now());
        <#continue>
    </#if>
        set${field.propertyName?cap_first}(<#if field.comment!?contains("${enumStart}")>${field.propertyName?cap_first}.random()<#else>random${field.propertyType}()</#if>);
</#list>
        return this;
    }

<#---------- END 添加Entity的randomEntity方法 ---------->
<#---------- START 添加Entity的tableName方法，避免依赖 MyBatis-Plus 注解而从 iv3Entity 接口契约中推断元数据 ---------->
    @Override
    public String tableName() {
        return "${table.name}";
    }

<#---------- END 添加Entity的tableName方法 ---------->
    public static void main(String[] args) {
        System.out.println(new V3${entity}().randomEntity());
    }

}
