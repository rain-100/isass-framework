    // region 所有类型都有的条件

    public ${entity}Criteria set${field.propertyName?cap_first}(${field.propertyType} ${field.propertyName}) {
        return equals(${entity}.${field.name?upper_case}, ${entity}.${field.name?upper_case}_COLUMN_NAME, ${field.propertyName});
    }

    public ${entity}Criteria setOr${field.propertyName?cap_first}(${field.propertyType} or${field.propertyName?cap_first}) {
        return orEquals(${entity}.${field.name?upper_case}, ${entity}.${field.name?upper_case}_COLUMN_NAME, or${field.propertyName?cap_first});
    }

    public ${entity}Criteria set${field.propertyName?cap_first}NotEqual(${field.propertyType} ${field.propertyName}NotEqual) {
        return notEquals(${entity}.${field.name?upper_case}, ${entity}.${field.name?upper_case}_COLUMN_NAME, ${field.propertyName}NotEqual);
    }

    public ${entity}Criteria setOr${field.propertyName?cap_first}NotEqual(${field.propertyType} or${field.propertyName?cap_first}NotEqual) {
        return orNotEquals(${entity}.${field.name?upper_case}, ${entity}.${field.name?upper_case}_COLUMN_NAME, or${field.propertyName?cap_first}NotEqual);
    }

    public ${entity}Criteria set${field.propertyName?cap_first}In(Collection<${field.propertyType}> ${field.propertyName}s) {
        return in(${entity}.${field.name?upper_case}, ${entity}.${field.name?upper_case}_COLUMN_NAME, ${field.propertyName}s);
    }

    public ${entity}Criteria setOr${field.propertyName?cap_first}In(Collection<${field.propertyType}> ${field.propertyName}s) {
        return orIn(${entity}.${field.name?upper_case}, ${entity}.${field.name?upper_case}_COLUMN_NAME, ${field.propertyName}s);
    }

    public ${entity}Criteria set${field.propertyName?cap_first}NotIn(Collection<${field.propertyType}> ${field.propertyName}s) {
        return notIn(${entity}.${field.name?upper_case}, ${entity}.${field.name?upper_case}_COLUMN_NAME, ${field.propertyName}s);
    }

    public ${entity}Criteria setOr${field.propertyName?cap_first}NotIn(Collection<${field.propertyType}> ${field.propertyName}s) {
        return orNotIn(${entity}.${field.name?upper_case}, ${entity}.${field.name?upper_case}_COLUMN_NAME, ${field.propertyName}s);
    }

    @JsonIgnore
    public ${entity}Criteria set${field.propertyName?cap_first}In(${field.propertyType}... ${field.propertyName}s) {
        return in(${entity}.${field.name?upper_case}, ${entity}.${field.name?upper_case}_COLUMN_NAME, CollUtil.newArrayList(${field.propertyName}s));
    }

    @JsonIgnore
    public ${entity}Criteria setOr${field.propertyName?cap_first}In(${field.propertyType}... ${field.propertyName}s) {
        return orIn(${entity}.${field.name?upper_case}, ${entity}.${field.name?upper_case}_COLUMN_NAME, CollUtil.newArrayList(${field.propertyName}s));
    }

    @JsonIgnore
    public ${entity}Criteria set${field.propertyName?cap_first}NotIn(${field.propertyType}... ${field.propertyName}s) {
        return notIn(${entity}.${field.name?upper_case}, ${entity}.${field.name?upper_case}_COLUMN_NAME, CollUtil.newArrayList(${field.propertyName}s));
    }

    @JsonIgnore
    public ${entity}Criteria setOr${field.propertyName?cap_first}NotIn(${field.propertyType}... ${field.propertyName}s) {
        return orNotIn(${entity}.${field.name?upper_case}, ${entity}.${field.name?upper_case}_COLUMN_NAME, CollUtil.newArrayList(${field.propertyName}s));
    }

    // endregion

