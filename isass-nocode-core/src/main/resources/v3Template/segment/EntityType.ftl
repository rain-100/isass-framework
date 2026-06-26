<#assign buildInColumns = [
cfg.idEntity.ID_COLUMN_NAME,
cfg.parentIdEntity.PARENT_ID_COLUMN_NAME,
cfg.logicDeleteEntity.DELETE_FLAG_COLUMN_NAME,
cfg.tenantEntity.TENANT_ID_COLUMN_NAME,
cfg.traceEntity.CREATE_USER_ID_COLUMN_NAME,
cfg.traceEntity.CREATE_USER_NAME_COLUMN_NAME,
cfg.traceEntity.CREATED_TIME_COLUMN_NAME,
cfg.traceEntity.MODIFY_USER_ID_COLUMN_NAME,
cfg.traceEntity.MODIFY_USER_NAME_COLUMN_NAME,
cfg.traceEntity.MODIFY_TIME_COLUMN_NAME,
cfg.versionEntity.VERSION_COLUMN_NAME
]>
<#------------ BEGIN IdEntity ------------>
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
    <#if field.name?lower_case == cfg.logicDeleteEntity.DELETE_FLAG_COLUMN_NAME>
        <#assign isLogicDeleteEntity = true>
        <#break>
    </#if>
    <#assign isLogicDeleteEntity = false>
</#list>
<#------------ END LogicDeleteEntity ------------>
<#------------ BEGIN ParentIdEntity ------------>
<#list table.fields as field>
    <#if field.name?lower_case == cfg.parentIdEntity.PARENT_ID_COLUMN_NAME>
        <#assign isParentIdEntity = true>
        <#assign parentIdEntityPropertyType = field.propertyType>
        <#break>
    </#if>
    <#assign isParentIdEntity = false>
</#list>
<#------------ END ParentIdEntity ------------>
<#------------ BEGIN TenantEntity ------------>
<#list table.fields as field>
    <#if field.name?lower_case == cfg.tenantEntity.TENANT_ID_COLUMN_NAME>
        <#assign isTenantEntity = true>
        <#assign tenantIdEntityPropertyType = field.propertyType>
        <#break>
    </#if>
    <#assign isTenantEntity = false>
</#list>
<#------------ END TenantEntity ------------>
<#------------ BEGIN TraceEntity ------------>
<#list table.fields as field>
<#if field.name?lower_case == cfg.traceEntity.CREATE_USER_ID_COLUMN_NAME
|| field.name?lower_case == cfg.traceEntity.CREATE_USER_NAME_COLUMN_NAME
|| field.name?lower_case == cfg.traceEntity.CREATED_TIME_COLUMN_NAME
|| field.name?lower_case == cfg.traceEntity.MODIFY_USER_ID_COLUMN_NAME
|| field.name?lower_case == cfg.traceEntity.MODIFY_USER_NAME_COLUMN_NAME
|| field.name?lower_case == cfg.traceEntity.MODIFY_TIME_COLUMN_NAME>
<#assign isTraceEntity = true>
<#break>
</#if>
<#assign isTraceEntity = false>
</#list>
<#------------ END traceEntity ------------>
<#------------ BEGIN VersionEntity ------------>
<#list table.fields as field>
<#if field.name?lower_case == cfg.versionEntity.VERSION_COLUMN_NAME>
<#assign isVersionEntity = true>
<#break>
</#if>
<#assign isVersionEntity = false>
</#list>
<#------------ END VersionEntity ------------>