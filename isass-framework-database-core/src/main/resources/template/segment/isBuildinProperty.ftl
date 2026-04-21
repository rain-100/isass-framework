<#assign isBuildinProperty = false>
<#if field.keyFlag><#assign isBuildinProperty = true>
</#if>
<#if versionFieldName == field.name><#assign isBuildinProperty = true>
</#if>
<#if logicDeleteFieldName == field.name><#assign isBuildinProperty = true>
</#if>
<#if field.propertyName == cfg.userTracedEntity.CREATE_USER_ID_PROPERTY
|| field.propertyName == cfg.userTracedEntity.CREATE_USER_NAME_PROPERTY
|| field.propertyName == cfg.timeTracedEntity.CREATED_TIME_PROPERTY><#assign isBuildinProperty = true>
</#if>
<#if field.propertyName == cfg.userTracedEntity.MODIFY_USER_ID_PROPERTY
|| field.propertyName == cfg.userTracedEntity.MODIFY_USER_NAME_PROPERTY
|| field.propertyName == cfg.timeTracedEntity.MODIFY_TIME_PROPERTY><#assign isBuildinProperty = true>
</#if>