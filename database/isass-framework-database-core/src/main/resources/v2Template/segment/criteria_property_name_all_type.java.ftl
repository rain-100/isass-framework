<#if swagger2>    @ApiModelProperty("<#if field.comment??>${field.comment}<#else>${field.propertyName}</#if>等于")</#if>
    private ${field.propertyType} ${field.propertyName};

<#if swagger2>    @ApiModelProperty(hidden = true, value="或者<#if field.comment??>${field.comment}<#else>${field.propertyName}</#if>等于")</#if>
    private ${field.propertyType} or${field.propertyName?cap_first};

<#if swagger2>    @ApiModelProperty(hidden = true, value="<#if field.comment??>${field.comment}<#else>${field.propertyName}</#if>不等于")</#if>
    private ${field.propertyType} ${field.propertyName}NotEqual;

<#if swagger2>    @ApiModelProperty(hidden = true, value="或者<#if field.comment??>${field.comment}<#else>${field.propertyName}</#if>不等于")</#if>
    private ${field.propertyType} or${field.propertyName?cap_first}NotEqual;

<#if swagger2>    @ApiModelProperty(hidden = true, value="<#if field.comment??>${field.comment}<#else>${field.propertyName}</#if>所在范围")</#if>
    private Collection<${field.propertyType}> ${field.propertyName}In;

<#if swagger2>    @ApiModelProperty(hidden = true, value="或者<#if field.comment??>${field.comment}<#else>${field.propertyName}</#if>所在范围")</#if>
    private Collection<${field.propertyType}> or${field.propertyName?cap_first}In;

<#if swagger2>    @ApiModelProperty(hidden = true, value="<#if field.comment??>${field.comment}<#else>${field.propertyName}</#if>不在范围")</#if>
    private Collection<${field.propertyType}> ${field.propertyName}NotIn;

<#if swagger2>    @ApiModelProperty(hidden = true, value="或者<#if field.comment??>${field.comment}<#else>${field.propertyName}</#if>不在范围")</#if>
    private Collection<${field.propertyType}> or${field.propertyName?cap_first}NotIn;

