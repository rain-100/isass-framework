<#assign hasBuildinProperty = false>
<#list table.fields as field>
<#if field.keyFlag><#assign hasBuildinProperty = true>
<#break>
</#if>
<#if versionFieldName == field.name><#assign hasBuildinProperty = true>
<#break>
</#if>
<#if logicDeleteFieldName == field.name><#assign hasBuildinProperty = true>
<#break>
</#if>
<#if field.propertyName == cfg.userTracedEntity.CREATE_USER_ID_PROPERTY
|| field.propertyName == cfg.userTracedEntity.CREATE_USER_NAME_PROPERTY
|| field.propertyName == cfg.timeTracedEntity.CREATED_TIME_PROPERTY><#assign hasBuildinProperty = true>
<#break>
</#if>
<#if field.propertyName == cfg.userTracedEntity.MODIFY_USER_ID_PROPERTY
|| field.propertyName == cfg.userTracedEntity.MODIFY_USER_NAME_PROPERTY
|| field.propertyName == cfg.timeTracedEntity.MODIFY_TIME_PROPERTY><#assign hasBuildinProperty = true>
<#break>
</#if>
</#list>