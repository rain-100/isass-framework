<#if field.propertyType == "String">
<#if swagger2>    @ApiModelProperty(hidden = true, value="<#if field.comment??>${field.comment}<#else>${field.propertyName}</#if>包含字符")</#if>
    private ${field.propertyType} ${field.propertyName}Like;

<#if swagger2>    @ApiModelProperty(hidden = true, value="或者<#if field.comment??>${field.comment}<#else>${field.propertyName}</#if>包含字符")</#if>
    private ${field.propertyType} or${field.propertyName?cap_first}Like;

<#if swagger2>    @ApiModelProperty(hidden = true, value="<#if field.comment??>${field.comment}<#else>${field.propertyName}</#if>不包含字符")</#if>
    private ${field.propertyType} ${field.propertyName}NotLike;

<#if swagger2>    @ApiModelProperty(hidden = true, value="或者<#if field.comment??>${field.comment}<#else>${field.propertyName}</#if>不包含字符")</#if>
    private ${field.propertyType} or${field.propertyName?cap_first}NotLike;

<#if swagger2>    @ApiModelProperty(hidden = true, value="<#if field.comment??>${field.comment}<#else>${field.propertyName}</#if>开始以")</#if>
    private ${field.propertyType} ${field.propertyName}StartWith;

<#if swagger2>    @ApiModelProperty(hidden = true, value="或者<#if field.comment??>${field.comment}<#else>${field.propertyName}</#if>开始以")</#if>
    private ${field.propertyType} or${field.propertyName?cap_first}StartWith;

</#if>