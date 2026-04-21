<#if field.propertyType == "Collection<String>">
<#if swagger2>    @ApiModelProperty(hidden = true, value="<#if field.comment??>${field.comment}<#else>${field.propertyName}</#if>包含全部")</#if>
    private ${field.propertyType} ${field.propertyName}ContainsAll;

<#if swagger2>    @ApiModelProperty(hidden = true, value="<#if field.comment??>${field.comment}<#else>${field.propertyName}</#if>包含任意")</#if>
    private ${field.propertyType} ${field.propertyName}ContainsAny;

</#if>