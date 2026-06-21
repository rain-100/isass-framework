<#if field.propertyType == "Integer"
|| field.propertyType == "Long"
|| field.propertyType == "BigDecimal"
|| field.propertyType == "Date"
|| field.propertyType == "LocalDate"
|| field.propertyType == "LocalTime"
|| field.propertyType == "LocalDateTime">
    private ${field.propertyType} ${field.propertyName}LessThan;

    private ${field.propertyType} or${field.propertyName?cap_first}LessThan;

    private ${field.propertyType} ${field.propertyName}LessThanEqual;

    private ${field.propertyType} or${field.propertyName?cap_first}LessThanEqual;

    private ${field.propertyType} ${field.propertyName}GreaterThan;

    private ${field.propertyType} or${field.propertyName?cap_first}GreaterThan;

    private ${field.propertyType} ${field.propertyName}GreaterThanEqual;

    private ${field.propertyType} or${field.propertyName?cap_first}GreaterThanEqual;

</#if>