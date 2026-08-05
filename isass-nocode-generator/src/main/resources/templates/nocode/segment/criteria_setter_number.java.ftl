<#if field.propertyType == "Integer"
|| field.propertyType == "Long"
|| field.propertyType == "BigDecimal"
|| field.propertyType == "Date"
|| field.propertyType == "LocalDate"
|| field.propertyType == "LocalTime"
|| field.propertyType == "LocalDateTime">
    // region 数字类型字段拥有的条件

    public ${entity}Criteria set${field.propertyName?cap_first}LessThan(${field.propertyType} ${field.propertyName}LessThan) {
        return isConditionValuePresent(${field.propertyName}LessThan) ? lessThan(propertyName(${entity}::get${field.propertyName?cap_first}), ${field.propertyName}LessThan) : this;
    }

    public ${entity}Criteria setOr${field.propertyName?cap_first}LessThan(${field.propertyType} or${field.propertyName?cap_first}LessThan) {
        return isConditionValuePresent(or${field.propertyName?cap_first}LessThan) ? orLessThan(propertyName(${entity}::get${field.propertyName?cap_first}), or${field.propertyName?cap_first}LessThan) : this;
    }

    public ${entity}Criteria set${field.propertyName?cap_first}LessThanEqual(${field.propertyType} ${field.propertyName}LessThanEqual) {
        return isConditionValuePresent(${field.propertyName}LessThanEqual) ? lessThanEqual(propertyName(${entity}::get${field.propertyName?cap_first}), ${field.propertyName}LessThanEqual) : this;
    }

    public ${entity}Criteria setOr${field.propertyName?cap_first}LessThanEqual(${field.propertyType} or${field.propertyName?cap_first}LessThanEqual) {
        return isConditionValuePresent(or${field.propertyName?cap_first}LessThanEqual) ? orLessThanEqual(propertyName(${entity}::get${field.propertyName?cap_first}), or${field.propertyName?cap_first}LessThanEqual) : this;
    }

    public ${entity}Criteria set${field.propertyName?cap_first}GreaterThan(${field.propertyType} ${field.propertyName}GreaterThan) {
        return isConditionValuePresent(${field.propertyName}GreaterThan) ? greaterThan(propertyName(${entity}::get${field.propertyName?cap_first}), ${field.propertyName}GreaterThan) : this;
    }

    public ${entity}Criteria setOr${field.propertyName?cap_first}GreaterThan(${field.propertyType} or${field.propertyName?cap_first}GreaterThan) {
        return isConditionValuePresent(or${field.propertyName?cap_first}GreaterThan) ? orGreaterThan(propertyName(${entity}::get${field.propertyName?cap_first}), or${field.propertyName?cap_first}GreaterThan) : this;
    }

    public ${entity}Criteria set${field.propertyName?cap_first}GreaterThanEqual(${field.propertyType} ${field.propertyName}GreaterThanEqual) {
        return isConditionValuePresent(${field.propertyName}GreaterThanEqual) ? greaterThanEqual(propertyName(${entity}::get${field.propertyName?cap_first}), ${field.propertyName}GreaterThanEqual) : this;
    }

    public ${entity}Criteria setOr${field.propertyName?cap_first}GreaterThanEqual(${field.propertyType} or${field.propertyName?cap_first}GreaterThanEqual) {
        return isConditionValuePresent(or${field.propertyName?cap_first}GreaterThanEqual) ? orGreaterThanEqual(propertyName(${entity}::get${field.propertyName?cap_first}), or${field.propertyName?cap_first}GreaterThanEqual) : this;
    }

    // endregion

</#if>
