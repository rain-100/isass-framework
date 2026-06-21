<#if field.propertyType == "String">
    private ${field.propertyType} ${field.propertyName}Like;

    private ${field.propertyType} or${field.propertyName?cap_first}Like;

    private ${field.propertyType} ${field.propertyName}NotLike;

    private ${field.propertyType} or${field.propertyName?cap_first}NotLike;

    private ${field.propertyType} ${field.propertyName}StartWith;

    private ${field.propertyType} or${field.propertyName?cap_first}StartWith;

</#if>