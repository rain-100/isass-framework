<#if field.propertyType == "Integer"
|| field.propertyType == "Long"
|| field.propertyType == "BigDecimal"
|| field.propertyType == "Date"
|| field.propertyType == "LocalDate"
|| field.propertyType == "LocalTime"
|| field.propertyType == "LocalDateTime">
<#if swagger2>    @ApiModelProperty(hidden = true, value="<#if field.comment??>${field.comment}<#else>${field.propertyName}</#if>小于")</#if>
    private ${field.propertyType} ${field.propertyName}LessThan;

<#if swagger2>    @ApiModelProperty(hidden = true, value="或者<#if field.comment??>${field.comment}<#else>${field.propertyName}</#if>小于")</#if>
    private ${field.propertyType} or${field.propertyName?cap_first}LessThan;

<#if swagger2>    @ApiModelProperty(hidden = true, value="<#if field.comment??>${field.comment}<#else>${field.propertyName}</#if>小于等于")</#if>
    private ${field.propertyType} ${field.propertyName}LessThanEqual;

<#if swagger2>    @ApiModelProperty(hidden = true, value="或者<#if field.comment??>${field.comment}<#else>${field.propertyName}</#if>小于等于")</#if>
    private ${field.propertyType} or${field.propertyName?cap_first}LessThanEqual;

<#if swagger2>    @ApiModelProperty(hidden = true, value="<#if field.comment??>${field.comment}<#else>${field.propertyName}</#if>大于")</#if>
    private ${field.propertyType} ${field.propertyName}GreaterThan;

<#if swagger2>    @ApiModelProperty(hidden = true, value="或者<#if field.comment??>${field.comment}<#else>${field.propertyName}</#if>大于")</#if>
    private ${field.propertyType} or${field.propertyName?cap_first}GreaterThan;

<#if swagger2>    @ApiModelProperty(hidden = true, value="<#if field.comment??>${field.comment}<#else>${field.propertyName}</#if>大于等于")</#if>
    private ${field.propertyType} ${field.propertyName}GreaterThanEqual;

<#if swagger2>    @ApiModelProperty(hidden = true, value="或者<#if field.comment??>${field.comment}<#else>${field.propertyName}</#if>大于等于")</#if>
    private ${field.propertyType} or${field.propertyName?cap_first}GreaterThanEqual;

</#if>