<#------------ BEGIN IdEntity ------------>
<#assign buildInColumns = [
cfg.idEntity.ID_COLUMN_NAME,
cfg.chainedEntity.PARENT_ID_COLUMN_NAME,
cfg.userTracedEntity.CREATE_USER_ID,
cfg.userTracedEntity.CREATE_USER_NAME,
cfg.userTracedEntity.MODIFY_USER_ID,
cfg.userTracedEntity.MODIFY_USER_NAME,
cfg.timeTracedEntity.CREATED_TIME,
cfg.timeTracedEntity.MODIFY_TIME,
cfg.versionEntity.VERSION,
cfg.logicDeleteEntity.DELETE_FLAG
]>
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
<#------------ BEGIN ChainedEntity ------------>
<#list table.fields as field>
<#if field.name?lower_case == cfg.chainedEntity.PARENT_ID_COLUMN_NAME>
<#assign isChainedEntity = true>
<#assign chainedEntityPropertyType = field.propertyType>
<#break>
</#if>
<#assign isChainedEntity = false>
</#list>
<#------------ END ChainedEntity ------------>
<#------------ BEGIN UserTracedEntity ------------>
<#list table.fields as field>
<#if field.name?lower_case == cfg.userTracedEntity.CREATE_USER_ID>
<#assign isUserTracedEntity = true>
<#assign userTracedEntityPropertyType = field.propertyType>
<#break>
</#if>
<#assign isUserTracedEntity = false>
</#list>
<#------------ END UserTracedEntity ------------>
<#------------ BEGIN TimeTracedEntity ------------>
<#list table.fields as field>
<#if field.name?lower_case == cfg.timeTracedEntity.CREATED_TIME>
<#assign isTimeTracedEntity = true>
<#break>
</#if>
<#assign isTimeTracedEntity = false>
</#list>
<#------------ END TimeTracedEntity ------------>
<#------------ BEGIN VersionEntity ------------>
<#list table.fields as field>
<#if field.name?lower_case == cfg.versionEntity.VERSION>
<#assign isVersionEntity = true>
<#break>
</#if>
<#assign isVersionEntity = false>
</#list>
<#------------ END VersionEntity ------------>
<#------------ BEGIN LogicDeleteEntity ------------>
<#list table.fields as field>
<#if field.name?lower_case == cfg.logicDeleteEntity.DELETE_FLAG>
<#assign isLogicDeleteEntity = true>
<#break>
</#if>
<#assign isLogicDeleteEntity = false>
</#list>
<#------------ END LogicDeleteEntity ------------>