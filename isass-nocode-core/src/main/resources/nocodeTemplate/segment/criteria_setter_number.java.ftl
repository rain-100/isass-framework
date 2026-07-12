<#if field.propertyType == "Integer"
|| field.propertyType == "Long"
|| field.propertyType == "BigDecimal"
|| field.propertyType == "Date"
|| field.propertyType == "LocalDate"
|| field.propertyType == "LocalTime"
|| field.propertyType == "LocalDateTime">
    // region 数字类型字段拥有的条件

    public ${entity}Criteria set${field.propertyName?cap_first}LessThan(${field.propertyType} ${field.propertyName}LessThan) {
        return lessThan(${entity}.${field.name?upper_case}, ${entity}.${field.name?upper_case}_COLUMN_NAME, ${field.propertyName}LessThan);
    }

    public ${entity}Criteria setOr${field.propertyName?cap_first}LessThan(${field.propertyType} or${field.propertyName?cap_first}LessThan) {
        return orLessThan(${entity}.${field.name?upper_case}, ${entity}.${field.name?upper_case}_COLUMN_NAME, or${field.propertyName?cap_first}LessThan);
    }

    public ${entity}Criteria set${field.propertyName?cap_first}LessThanEqual(${field.propertyType} ${field.propertyName}LessThanEqual) {
        return lessThanEqual(${entity}.${field.name?upper_case}, ${entity}.${field.name?upper_case}_COLUMN_NAME, ${field.propertyName}LessThanEqual);
    }

    public ${entity}Criteria setOr${field.propertyName?cap_first}LessThanEqual(${field.propertyType} or${field.propertyName?cap_first}LessThanEqual) {
        return orLessThanEqual(${entity}.${field.name?upper_case}, ${entity}.${field.name?upper_case}_COLUMN_NAME, or${field.propertyName?cap_first}LessThanEqual);
    }

    public ${entity}Criteria set${field.propertyName?cap_first}GreaterThan(${field.propertyType} ${field.propertyName}GreaterThan) {
        return greaterThan(${entity}.${field.name?upper_case}, ${entity}.${field.name?upper_case}_COLUMN_NAME, ${field.propertyName}GreaterThan);
    }

    public ${entity}Criteria setOr${field.propertyName?cap_first}GreaterThan(${field.propertyType} or${field.propertyName?cap_first}GreaterThan) {
        return orGreaterThan(${entity}.${field.name?upper_case}, ${entity}.${field.name?upper_case}_COLUMN_NAME, or${field.propertyName?cap_first}GreaterThan);
    }

    public ${entity}Criteria set${field.propertyName?cap_first}GreaterThanEqual(${field.propertyType} ${field.propertyName}GreaterThanEqual) {
        return greaterThanEqual(${entity}.${field.name?upper_case}, ${entity}.${field.name?upper_case}_COLUMN_NAME, ${field.propertyName}GreaterThanEqual);
    }

    public ${entity}Criteria setOr${field.propertyName?cap_first}GreaterThanEqual(${field.propertyType} or${field.propertyName?cap_first}GreaterThanEqual) {
        return orGreaterThanEqual(${entity}.${field.name?upper_case}, ${entity}.${field.name?upper_case}_COLUMN_NAME, or${field.propertyName?cap_first}GreaterThanEqual);
    }

    // endregion

</#if>