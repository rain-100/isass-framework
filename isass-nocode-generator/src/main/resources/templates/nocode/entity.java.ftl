<#include "./segment/copyright.ftl">

<#assign enumStart = "[枚举--">
<#assign javaTypeStart = "[javaType--">
<#include "./segment/EntityType.ftl">
package ${cfg.entityPackageName};
<#function javaType field>
<#if field.comment!?contains("${javaTypeStart}")>
    <#assign start = field.comment?index_of("${javaTypeStart}") + javaTypeStart?length>
    <#assign end = field.comment?index_of("]", start)>
    <#return field.comment?substring(start, end)?trim>
</#if>
<#return field.propertyType>
</#function>

<#list table.fields as field>
<#if field.comment!?contains("${enumStart}")>
import cn.hutool.core.util.RandomUtil;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
<#break>
</#if>
</#list>
<#list table.fields as field>
<#if field.propertyType == "JsonNode" && !field.comment!?contains("${javaTypeStart}")>
import tools.jackson.databind.JsonNode;
<#break>
</#if>
</#list>
<#list table.fields as field>
<#if javaType(field)?starts_with("List<")>
import java.util.List;
<#break>
</#if>
</#list>
<#list table.fields as field>
<#if javaType(field)?starts_with("Map<")>
import java.util.Map;
<#break>
</#if>
</#list>
<#list table.fields as field>
<#if field.propertyName!?ends_with("Id") && field.propertyType == "Long">
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;
<#break>
</#if>
</#list>
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import vip.isass.framework.nocode.entity.IEntity;
import vip.isass.framework.entrypoint.annotation.ApiDoc;
<#if associations?has_content || isParentIdEntity>
import vip.isass.framework.nocode.entity.EntityAssociation;
</#if>
<#if isIdEntity>
import vip.isass.framework.nocode.entity.IIdEntity;
</#if>
<#if isLogicDeleteEntity>
import vip.isass.framework.nocode.entity.ILogicDeleteEntity;
</#if>
<#if isParentIdEntity>
import vip.isass.framework.nocode.entity.IParentIdEntity;
</#if>
<#if isTenantEntity>
import vip.isass.framework.nocode.entity.ITenantEntity;
</#if>
<#if isTraceEntity>
import vip.isass.framework.nocode.entity.ITraceEntity;
</#if>
<#if isVersionEntity>
import vip.isass.framework.nocode.entity.IVersionEntity;
</#if>
<#list table.fields as field>
<#if (field.propertyType == "LocalDate"
|| field.propertyType == "LocalTime"
|| field.propertyType == "LocalDateTime")
&& field.name?lower_case != "create_time"
&& field.name?lower_case != "modify_time">
import vip.isass.framework.common.support.LocalDateTimeUtil;
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
<#if associations?has_content || isParentIdEntity>
import java.util.Collection;
import java.util.List;
</#if>

<#------------ BEGIN 定义类名 ------------>
/**
 * <p>
 * <#if table.comment?trim?length gt 0>${table.comment}<#else>${entity}</#if> 数据模型
 * </p>
 *
 * @author ${author}
 */
@ApiDoc(description = "<#if table.comment?trim?length gt 0>${table.comment?j_string}<#else>${entity?j_string}</#if>")
@Getter
@Setter
@ToString
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ${entity} implements
<#if isIdEntity>
        IIdEntity<${idEntityPropertyType}, ${entity}>,
</#if>
<#if isParentIdEntity>
        IParentIdEntity<${parentIdEntityPropertyType}, ${entity}>,
</#if>
<#if isVersionEntity>
        IVersionEntity<${entity}>,
</#if>
<#if isTenantEntity>
        ITenantEntity<${tenantIdEntityPropertyType}, ${entity}>,
</#if>
<#if isLogicDeleteEntity>
        ILogicDeleteEntity<${entity}>,
</#if>
<#if isTraceEntity>
        ITraceEntity<${idEntityPropertyType}, ${entity}>,
</#if>
        IEntity<${entity}> {

<#------------ END 定义类名 ------------>
<#------------ BEGIN 定义公共字段 ------------>
    public static final ${entity} EMPTY = new ${entity}();

    /** 数据库表注释，用于管理端、初始化数据和接口文档展示。 */
    public transient static final String COMMENT = "${tableDescription?j_string}";

    private static final long serialVersionUID = 1L;

<#------------ END 定义公共字段 ------------>
<#---------- BEGIN 定义字段 ------------>
<#list table.fields as field>
    /**
     * <p>
     * <#if (field.comment?trim?length > 0)>${field.comment}<#else>${field.propertyName}</#if>
     * </p>
     * 数据库字段名: ${field.name}
     * 数据库字段类型: ${field.metaInfo.typeName}
     */<#if field.propertyName!?ends_with("Id") && field.propertyType == "Long">
    @JsonSerialize(using = ToStringSerializer.class)</#if>
    @ApiDoc(description = "<#if field.comment?trim?length gt 0>${field.comment?trim?j_string}<#else>${field.propertyName?j_string}</#if>")
    private <#if field.propertyName == "deleteFlag">Boolean<#elseif field.comment!?contains("${enumStart}")>${field.propertyName?cap_first}<#elseif field.comment!?contains("${javaTypeStart}")>${javaType(field)}<#elseif field.propertyType == "JsonNode">JsonNode<#else>${field.propertyType}</#if> ${field.propertyName};

    public void set${field.propertyName?cap_first}(<#if field.propertyName == "deleteFlag">Boolean<#elseif field.comment!?contains("${enumStart}")>${field.propertyName?cap_first}<#elseif field.comment!?contains("${javaTypeStart}")>${javaType(field)}<#elseif field.propertyType == "JsonNode">JsonNode<#else>${field.propertyType}</#if> ${field.propertyName}) {
        this.${field.propertyName} = ${field.propertyName};
        markPresentProperty("${field.propertyName}");
    }

</#list>
<#---------- END 定义字段 ---------->
<#list associations as association>
<#if association.kind()?string == "MANY">
    private Collection<${association.targetEntity()}> ${association.property()};

    public void set${association.property()?cap_first}(Collection<${association.targetEntity()}> ${association.property()}) {
        this.${association.property()} = ${association.property()};
        markPresentProperty("${association.property()}");
    }
<#else>
    private ${association.targetEntity()} ${association.property()};

    public void set${association.property()?cap_first}(${association.targetEntity()} ${association.property()}) {
        this.${association.property()} = ${association.property()};
        markPresentProperty("${association.property()}");
    }
</#if>

</#list>
<#if isParentIdEntity>
    private ${entity} parent;

    public void setParent(${entity} parent) {
        this.parent = parent;
        markPresentProperty("parent");
    }

    private List<${entity}> children;

    public void setChildren(List<${entity}> children) {
        this.children = children;
        markPresentProperty("children");
    }

</#if>
<#if associations?has_content || isParentIdEntity>
    @Override
    public List<EntityAssociation> associations() {
        return List.of(
<#list associations as association>
                EntityAssociation.${(association.kind()?string == "MANY")?then("many", "one")}("${association.property()}", ${association.targetEntity()}.class,
                        "${association.localKey()}", "${association.targetKey()}", ${association.cascadeDelete()?c})<#if association_has_next || isParentIdEntity>,</#if>
</#list>
<#if isParentIdEntity>
                EntityAssociation.one("parent", ${entity}.class, "parentId", "id", false),
                EntityAssociation.many("children", ${entity}.class, "id", "parentId", ${treeCascadeDelete?c})
</#if>
        );
    }

</#if>
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
<#-- 当主键属性名与默认主键属性名不一致时，添加默认主键字段名的get、set方法。
     数据库元数据中的列名可能是大写的 ID，不能用列名判断默认主键。 -->
<#if idEntityPropertyName != "id">
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
    public ${entity} randomEntity() {
<#if isIdEntity>
        IIdEntity.super.randomEntity();
</#if>
<#if isParentIdEntity>
        IParentIdEntity.super.randomEntity();
</#if>
<#if isVersionEntity>
        IVersionEntity.super.randomEntity();
</#if>
<#if isTenantEntity>
        ITenantEntity.super.randomEntity();
</#if>
<#if isLogicDeleteEntity>
        ILogicDeleteEntity.super.randomEntity();
</#if>
<#if isTraceEntity>
        ITraceEntity.super.randomEntity();
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
        System.out.println(new ${entity}().randomEntity());
    }

}
