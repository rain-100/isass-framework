<#include "./segment/copyright.ftl">

<#assign enumStart = "[枚举--">
<#include "./segment/EntityType.ftl">
package ${cfg.entityPackageName};

<#if swagger2>
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
</#if>
<#list table.fields as field>
<#if field.comment!?contains(enumStart)>
import cn.hutool.core.util.RandomUtil;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
<#break>
</#if>
</#list>
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import vip.isass.core.entity.*;
import vip.isass.core.sequence.impl.LongSequence;
import vip.isass.core.support.JsonUtil;
<#list table.fields as field>
<#if (field.propertyType == "LocalDate"
|| field.propertyType == "LocalTime"
|| field.propertyType == "LocalDateTime")
&& field.name != cfg.timeTracedEntity.CREATED_TIME
&& field.name != cfg.timeTracedEntity.MODIFY_TIME>
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
 * ${table.comment!}
 * </p>
 *
 * @author ${author}
 */
@Getter
@Setter
@Accessors(chain = true)
<#if swagger2>@ApiModel("<#if table.comment??>${table.comment}<#else>${entity}</#if>")</#if>
public class ${entity} implements
<#if isIdEntity>
        IdEntity<${idEntityPropertyType}, ${entity}>,
</#if>
<#if isChainedEntity>
        ChainedEntity<${chainedEntityPropertyType}, ${entity}>,
</#if>
<#if isUserTracedEntity>
        UserTracedEntity<${userTracedEntityPropertyType}, ${entity}>,
</#if>
<#if isTimeTracedEntity>
        TimeTracedEntity<${entity}>,
</#if>
<#if isVersionEntity>
        VersionEntity<${entity}>,
</#if>
<#if isLogicDeleteEntity>
        LogicDeleteEntity<${entity}>,
</#if>
        IEntity<${entity}> {

<#------------ END 定义类名 ------------>
<#------------ BEGIN 定义公共字段 ------------>
    private static final long serialVersionUID = 1L;

<#------------ END 定义公共字段 ------------>
<#------------ BEGIN 定义数据库字段名 ------------>
<#list table.fields as field>
<#if field.keyFlag && idEntityColumnName?lower_case != cfg.idEntity.ID_COLUMN_NAME?lower_case>
    public transient static final String ID = "${idEntityColumnName}";

</#if>
    public transient static final String ${field.name?upper_case} = "${field.name}";

</#list>
<#---------- BEGIN 定义字段 ------------>
<#list table.fields as field>
    /**
<#if field.comment??>
     * <p>
     * ${field.comment}
     * </p>
</#if>
     * 数据库字段名: ${field.name}
     * 数据库字段类型: ${field.type}
     */
<#if swagger2 || field.propertyType == "Json">    @ApiModelProperty(value = "<#if field.comment??>${field.comment}<#else>${field.propertyName?cap_first}</#if>")</#if>
    private <#if field.comment!?contains("${enumStart}")>${field.propertyName?cap_first} <#else>${field.propertyType} </#if>${field.propertyName};

</#list>
<#---------- END 定义字段 ---------->
<#---------- START 添加枚举类 ---------->
<#list table.fields as field>
    <#if field.comment!?contains("${enumStart}")>
        <#assign start = field.comment?index_of("${enumStart}") + 5>
        <#assign end = field.comment?index_of("]", start)>
        <#assign enumStringArr = field.comment?substring(start, end)?split(";")>
    public enum ${field.propertyName?cap_first} {

<#list enumStringArr as enumString>
<#assign enumArr = enumString?split(":")>
        ${enumArr[1]}(${enumArr[0]}, "${enumArr[2]}")<#if (enumString_index + 1) == enumStringArr?size>;<#else>,</#if>
        </#list>

        private Integer code;

        @Getter
        private String desc;

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
    public ${entity} setId(${idEntityPropertyType} id) {
        this.${idEntityPropertyName} = id;
        return this;
    }

</#if>
    @Override
    @Transient
    public String getIdColumnName() {
        return ID;
    }

    @Override
    public ${entity} randomId() {
        setId(LongSequence.get()<#if idEntityPropertyType == "String">.toString()<#elseif idEntityPropertyType == "Integer">.intValue()</#if>);
        return this;
    }

</#if>
<#---------- END 添加IdEntity的方法 ---------->
<#---------- START 添加UserTracedEntity的方法 ---------->
<#if isUserTracedEntity>
    @Override
    public ${entity} randomUserTracedId() {
        setCreateUserId(LongSequence.get()<#if userTracedEntityPropertyType == "String">.toString()</#if>);
        setModifyUserId(LongSequence.get()<#if userTracedEntityPropertyType == "String">.toString()</#if>);
        return this;
    }

</#if>
<#---------- END 添加UserTracedEntity的方法 ---------->
<#---------- START 添加ChainedEntity的方法 ---------->
<#if isChainedEntity>
    @Override
    public ${entity} markAsTopEntity() {
        setParentId(Long.valueOf(TOP_ID_VALUE)<#if chainedEntityPropertyType == "String">.toString()</#if>);
        return this;
    }

</#if>
<#---------- END 添加ChainedEntity的方法 ---------->
<#---------- START 添加Entity的randomEntity方法 ---------->
    @Override
    public ${entity} randomEntity() {
<#if isIdEntity>
        IdEntity.super.randomEntity();
</#if>
<#if isChainedEntity>
        ChainedEntity.super.randomEntity();
</#if>
<#if isUserTracedEntity>
        UserTracedEntity.super.randomEntity();
</#if>
<#if isTimeTracedEntity>
        TimeTracedEntity.super.randomEntity();
</#if>
<#if isVersionEntity>
        VersionEntity.super.randomEntity();
</#if>
<#if isLogicDeleteEntity>
        LogicDeleteEntity.super.randomEntity();
</#if>
<#list table.fields as field>
    <#if field.keyFlag><#continue></#if>
    <#if field.propertyType?ends_with("[]")><#continue></#if>
    <#if field.propertyType == "Json"><#continue></#if>
    <#if field.propertyType == "Collection<String>"><#continue></#if>
    <#if buildInColumns?seq_contains(field.name)><#continue></#if>
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
<#---------- START 添加toString方法 ---------->
    @Override
    @SneakyThrows
    public String toString() {
        return JsonUtil.NOT_NULL_INSTANCE.writeValueAsString(this);
    }

<#---------- END 添加toString方法 ---------->
    public static void main(String[] args) {
        System.out.println(new ${entity}().randomEntity());
    }

}