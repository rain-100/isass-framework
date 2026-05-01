<#if field.propertyType == "Integer"
|| field.propertyType == "Long"
|| field.propertyType == "BigDecimal"
|| field.propertyType == "Date"
|| field.propertyType == "LocalDate"
|| field.propertyType == "LocalTime"
|| field.propertyType == "LocalDateTime">
    public ${entity}Criteria set${field.propertyName?cap_first}LessThan(${field.propertyType} ${field.propertyName}LessThan) {
        this.${field.propertyName}LessThan = ${field.propertyName}LessThan;
        lessThan(${entity}.${field.name?upper_case}, this.${field.propertyName}LessThan);
        return this;
    }

    public ${entity}Criteria setOr${field.propertyName?cap_first}LessThan(${field.propertyType} or${field.propertyName?cap_first}LessThan) {
        this.or${field.propertyName?cap_first}LessThan = or${field.propertyName?cap_first}LessThan;
        orLessThan(${entity}.${field.name?upper_case}, this.or${field.propertyName?cap_first}LessThan);
        return this;
    }

    public ${entity}Criteria set${field.propertyName?cap_first}LessThanEqual(${field.propertyType} ${field.propertyName}LessThanEqual) {
        this.${field.propertyName}LessThanEqual = ${field.propertyName}LessThanEqual;
        lessThanEqual(${entity}.${field.name?upper_case}, this.${field.propertyName}LessThanEqual);
        return this;
    }

    public ${entity}Criteria setOr${field.propertyName?cap_first}LessThanEqual(${field.propertyType} or${field.propertyName?cap_first}LessThanEqual) {
        this.or${field.propertyName?cap_first}LessThanEqual = or${field.propertyName?cap_first}LessThanEqual;
        orLessThanEqual(${entity}.${field.name?upper_case}, this.or${field.propertyName?cap_first}LessThanEqual);
        return this;
    }

    public ${entity}Criteria set${field.propertyName?cap_first}GreaterThan(${field.propertyType} ${field.propertyName}GreaterThan) {
        this.${field.propertyName}GreaterThan = ${field.propertyName}GreaterThan;
        greaterThan(${entity}.${field.name?upper_case}, this.${field.propertyName}GreaterThan);
        return this;
    }

    public ${entity}Criteria setOr${field.propertyName?cap_first}GreaterThan(${field.propertyType} or${field.propertyName?cap_first}GreaterThan) {
        this.or${field.propertyName?cap_first}GreaterThan = or${field.propertyName?cap_first}GreaterThan;
        orGreaterThan(${entity}.${field.name?upper_case}, this.or${field.propertyName?cap_first}GreaterThan);
        return this;
    }

    public ${entity}Criteria set${field.propertyName?cap_first}GreaterThanEqual(${field.propertyType} ${field.propertyName}GreaterThanEqual) {
        this.${field.propertyName}GreaterThanEqual = ${field.propertyName}GreaterThanEqual;
        greaterThanEqual(${entity}.${field.name?upper_case}, this.${field.propertyName}GreaterThanEqual);
        return this;
    }

    public ${entity}Criteria setOr${field.propertyName?cap_first}GreaterThanEqual(${field.propertyType} or${field.propertyName?cap_first}GreaterThanEqual) {
        this.or${field.propertyName?cap_first}GreaterThanEqual = or${field.propertyName?cap_first}GreaterThanEqual;
        orGreaterThanEqual(${entity}.${field.name?upper_case}, this.or${field.propertyName?cap_first}GreaterThanEqual);
        return this;
    }

</#if>