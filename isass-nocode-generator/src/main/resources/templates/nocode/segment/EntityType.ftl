<#assign buildInColumns = ["id", "parent_id", "delete_flag", "tenant_id", "create_user_id", "create_user_name", "create_time", "modify_user_id", "modify_user_name", "modify_time", "version"]>
<#------------ BEGIN IdEntity ------------>
<#assign isIdEntity = false>
<#assign idEntityPropertyType = "Serializable">
<#list table.fields as field>
<#if field.keyFlag>
<#assign isIdEntity = true>
<#assign idEntityPropertyType = field.propertyType>
<#assign idEntityColumnName = field.name>
<#assign idEntityPropertyName = field.propertyName>
<#break>
</#if>
<#assign isIdEntity = false>
</#list>
<#------------ END IdEntity ------------>
<#------------ BEGIN LogicDeleteEntity ------------>
<#list table.fields as field>
    <#if field.name?lower_case == "delete_flag">
        <#assign isLogicDeleteEntity = true>
        <#break>
    </#if>
    <#assign isLogicDeleteEntity = false>
</#list>
<#------------ END LogicDeleteEntity ------------>
<#------------ BEGIN ParentIdEntity ------------>
<#list table.fields as field>
    <#if isIdEntity && field.name?lower_case == "parent_id" && field.propertyType == idEntityPropertyType>
        <#assign isParentIdEntity = true>
        <#assign parentIdEntityPropertyType = field.propertyType>
        <#break>
    </#if>
    <#assign isParentIdEntity = false>
</#list>
<#------------ END ParentIdEntity ------------>
<#------------ BEGIN TenantEntity ------------>
<#list table.fields as field>
    <#if field.name?lower_case == "tenant_id" && tenantIsolation>
        <#assign isTenantEntity = true>
        <#assign tenantIdEntityPropertyType = field.propertyType>
        <#break>
    </#if>
    <#assign isTenantEntity = false>
</#list>
<#------------ END TenantEntity ------------>
<#------------ BEGIN TraceEntity ------------>
<#assign traceEntityPropertyType = "Long">
<#assign traceUserTypeFound = false>
<#list table.fields as field>
<#if field.name?lower_case == "create_user_id" || field.name?lower_case == "modify_user_id">
<#if traceUserTypeFound && traceEntityPropertyType != field.propertyType>
<#stop "create_user_id 与 modify_user_id 的 Java 类型必须一致">
</#if>
<#assign traceEntityPropertyType = field.propertyType>
<#assign traceUserTypeFound = true>
</#if>
</#list>
<#list table.fields as field>
<#if field.name?lower_case == "create_user_id"
|| field.name?lower_case == "create_user_name"
|| field.name?lower_case == "create_time"
|| field.name?lower_case == "modify_user_id"
|| field.name?lower_case == "modify_user_name"
|| field.name?lower_case == "modify_time">
<#assign isTraceEntity = true>
<#break>
</#if>
<#assign isTraceEntity = false>
</#list>
<#------------ END traceEntity ------------>
<#------------ BEGIN VersionEntity ------------>
<#list table.fields as field>
<#if field.name?lower_case == "version">
<#assign isVersionEntity = true>
<#break>
</#if>
<#assign isVersionEntity = false>
</#list>
<#------------ END VersionEntity ------------>
