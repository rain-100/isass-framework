    // region 所有类型都有的条件

    public V2${entity}Criteria set${field.propertyName?cap_first}(${field.propertyType} ${field.propertyName}) {
        return equals(V2${entity}.${field.name?upper_case}, V2${entity}.${field.name?upper_case}_COLUMN_NAME, ${field.propertyName});
    }

    public V2${entity}Criteria setOr${field.propertyName?cap_first}(${field.propertyType} or${field.propertyName?cap_first}) {
        return orEquals(V2${entity}.${field.name?upper_case}, V2${entity}.${field.name?upper_case}_COLUMN_NAME, or${field.propertyName?cap_first});
    }

    public V2${entity}Criteria set${field.propertyName?cap_first}NotEqual(${field.propertyType} ${field.propertyName}NotEqual) {
        return notEquals(V2${entity}.${field.name?upper_case}, V2${entity}.${field.name?upper_case}_COLUMN_NAME, ${field.propertyName}NotEqual);
    }

    public V2${entity}Criteria setOr${field.propertyName?cap_first}NotEqual(${field.propertyType} or${field.propertyName?cap_first}NotEqual) {
        return orNotEquals(V2${entity}.${field.name?upper_case}, V2${entity}.${field.name?upper_case}_COLUMN_NAME, or${field.propertyName?cap_first}NotEqual);
    }

    public V2${entity}Criteria set${field.propertyName?cap_first}In(Collection<${field.propertyType}> ${field.propertyName}s) {
        return in(V2${entity}.${field.name?upper_case}, V2${entity}.${field.name?upper_case}_COLUMN_NAME, ${field.propertyName}s);
    }

    public V2${entity}Criteria setOr${field.propertyName?cap_first}In(Collection<${field.propertyType}> ${field.propertyName}s) {
        return orIn(V2${entity}.${field.name?upper_case}, V2${entity}.${field.name?upper_case}_COLUMN_NAME, ${field.propertyName}s);
    }

    public V2${entity}Criteria set${field.propertyName?cap_first}NotIn(Collection<${field.propertyType}> ${field.propertyName}s) {
        return notIn(V2${entity}.${field.name?upper_case}, V2${entity}.${field.name?upper_case}_COLUMN_NAME, ${field.propertyName}s);
    }

    public V2${entity}Criteria setOr${field.propertyName?cap_first}NotIn(Collection<${field.propertyType}> ${field.propertyName}s) {
        return orNotIn(V2${entity}.${field.name?upper_case}, V2${entity}.${field.name?upper_case}_COLUMN_NAME, ${field.propertyName}s);
    }

    @JsonIgnore
    public V2${entity}Criteria set${field.propertyName?cap_first}In(${field.propertyType}... ${field.propertyName}s) {
        return in(V2${entity}.${field.name?upper_case}, V2${entity}.${field.name?upper_case}_COLUMN_NAME, CollUtil.newArrayList(${field.propertyName}s));
    }

    @JsonIgnore
    public V2${entity}Criteria setOr${field.propertyName?cap_first}In(${field.propertyType}... ${field.propertyName}s) {
        return orIn(V2${entity}.${field.name?upper_case}, V2${entity}.${field.name?upper_case}_COLUMN_NAME, CollUtil.newArrayList(${field.propertyName}s));
    }

    @JsonIgnore
    public V2${entity}Criteria set${field.propertyName?cap_first}NotIn(${field.propertyType}... ${field.propertyName}s) {
        return notIn(V2${entity}.${field.name?upper_case}, V2${entity}.${field.name?upper_case}_COLUMN_NAME, CollUtil.newArrayList(${field.propertyName}s));
    }

    @JsonIgnore
    public V2${entity}Criteria setOr${field.propertyName?cap_first}NotIn(${field.propertyType}... ${field.propertyName}s) {
        return orNotIn(V2${entity}.${field.name?upper_case}, V2${entity}.${field.name?upper_case}_COLUMN_NAME, CollUtil.newArrayList(${field.propertyName}s));
    }

    // endregion

