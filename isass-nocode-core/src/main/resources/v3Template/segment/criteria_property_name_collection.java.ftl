<#if field.propertyType == "Collection<String>">
    private ${field.propertyType} ${field.propertyName}ContainsAll;

    private ${field.propertyType} ${field.propertyName}ContainsAny;

</#if>