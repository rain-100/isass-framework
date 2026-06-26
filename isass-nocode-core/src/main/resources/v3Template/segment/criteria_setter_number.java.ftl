<#if field.propertyType == "Integer"
|| field.propertyType == "Long"
|| field.propertyType == "BigDecimal"
|| field.propertyType == "Date"
|| field.propertyType == "LocalDate"
|| field.propertyType == "LocalTime"
|| field.propertyType == "LocalDateTime">
    // region 数字类型字段拥有的条件

    public V3${entity}Criteria set${field.propertyName?cap_first}LessThan(${field.propertyType} ${field.propertyName}LessThan) {
        return lessThan(V3${entity}.${field.name?upper_case}, V3${entity}.${field.name?upper_case}_COLUMN_NAME, ${field.propertyName}LessThan);
    }

    public V3${entity}Criteria setOr${field.propertyName?cap_first}LessThan(${field.propertyType} or${field.propertyName?cap_first}LessThan) {
        return orLessThan(V3${entity}.${field.name?upper_case}, V3${entity}.${field.name?upper_case}_COLUMN_NAME, or${field.propertyName?cap_first}LessThan);
    }

    public V3${entity}Criteria set${field.propertyName?cap_first}LessThanEqual(${field.propertyType} ${field.propertyName}LessThanEqual) {
        return lessThanEqual(V3${entity}.${field.name?upper_case}, V3${entity}.${field.name?upper_case}_COLUMN_NAME, ${field.propertyName}LessThanEqual);
    }

    public V3${entity}Criteria setOr${field.propertyName?cap_first}LessThanEqual(${field.propertyType} or${field.propertyName?cap_first}LessThanEqual) {
        return orLessThanEqual(V3${entity}.${field.name?upper_case}, V3${entity}.${field.name?upper_case}_COLUMN_NAME, or${field.propertyName?cap_first}LessThanEqual);
    }

    public V3${entity}Criteria set${field.propertyName?cap_first}GreaterThan(${field.propertyType} ${field.propertyName}GreaterThan) {
        return greaterThan(V3${entity}.${field.name?upper_case}, V3${entity}.${field.name?upper_case}_COLUMN_NAME, ${field.propertyName}GreaterThan);
    }

    public V3${entity}Criteria setOr${field.propertyName?cap_first}GreaterThan(${field.propertyType} or${field.propertyName?cap_first}GreaterThan) {
        return orGreaterThan(V3${entity}.${field.name?upper_case}, V3${entity}.${field.name?upper_case}_COLUMN_NAME, or${field.propertyName?cap_first}GreaterThan);
    }

    public V3${entity}Criteria set${field.propertyName?cap_first}GreaterThanEqual(${field.propertyType} ${field.propertyName}GreaterThanEqual) {
        return greaterThanEqual(V3${entity}.${field.name?upper_case}, V3${entity}.${field.name?upper_case}_COLUMN_NAME, ${field.propertyName}GreaterThanEqual);
    }

    public V3${entity}Criteria setOr${field.propertyName?cap_first}GreaterThanEqual(${field.propertyType} or${field.propertyName?cap_first}GreaterThanEqual) {
        return orGreaterThanEqual(V3${entity}.${field.name?upper_case}, V3${entity}.${field.name?upper_case}_COLUMN_NAME, or${field.propertyName?cap_first}GreaterThanEqual);
    }

    // endregion

</#if>