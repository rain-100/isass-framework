    // region 所有类型都有的条件

    public V3${entity}Criteria set${field.propertyName?cap_first}(${field.propertyType} ${field.propertyName}) {
        return equals(V3${entity}.${field.name?upper_case}, V3${entity}.${field.name?upper_case}_COLUMN_NAME, ${field.propertyName});
    }

    public V3${entity}Criteria setOr${field.propertyName?cap_first}(${field.propertyType} or${field.propertyName?cap_first}) {
        return orEquals(V3${entity}.${field.name?upper_case}, V3${entity}.${field.name?upper_case}_COLUMN_NAME, or${field.propertyName?cap_first});
    }

    public V3${entity}Criteria set${field.propertyName?cap_first}NotEqual(${field.propertyType} ${field.propertyName}NotEqual) {
        return notEquals(V3${entity}.${field.name?upper_case}, V3${entity}.${field.name?upper_case}_COLUMN_NAME, ${field.propertyName}NotEqual);
    }

    public V3${entity}Criteria setOr${field.propertyName?cap_first}NotEqual(${field.propertyType} or${field.propertyName?cap_first}NotEqual) {
        return orNotEquals(V3${entity}.${field.name?upper_case}, V3${entity}.${field.name?upper_case}_COLUMN_NAME, or${field.propertyName?cap_first}NotEqual);
    }

    public V3${entity}Criteria set${field.propertyName?cap_first}In(Collection<${field.propertyType}> ${field.propertyName}s) {
        return in(V3${entity}.${field.name?upper_case}, V3${entity}.${field.name?upper_case}_COLUMN_NAME, ${field.propertyName}s);
    }

    public V3${entity}Criteria setOr${field.propertyName?cap_first}In(Collection<${field.propertyType}> ${field.propertyName}s) {
        return orIn(V3${entity}.${field.name?upper_case}, V3${entity}.${field.name?upper_case}_COLUMN_NAME, ${field.propertyName}s);
    }

    public V3${entity}Criteria set${field.propertyName?cap_first}NotIn(Collection<${field.propertyType}> ${field.propertyName}s) {
        return notIn(V3${entity}.${field.name?upper_case}, V3${entity}.${field.name?upper_case}_COLUMN_NAME, ${field.propertyName}s);
    }

    public V3${entity}Criteria setOr${field.propertyName?cap_first}NotIn(Collection<${field.propertyType}> ${field.propertyName}s) {
        return orNotIn(V3${entity}.${field.name?upper_case}, V3${entity}.${field.name?upper_case}_COLUMN_NAME, ${field.propertyName}s);
    }

    @JsonIgnore
    public V3${entity}Criteria set${field.propertyName?cap_first}In(${field.propertyType}... ${field.propertyName}s) {
        return in(V3${entity}.${field.name?upper_case}, V3${entity}.${field.name?upper_case}_COLUMN_NAME, CollUtil.newArrayList(${field.propertyName}s));
    }

    @JsonIgnore
    public V3${entity}Criteria setOr${field.propertyName?cap_first}In(${field.propertyType}... ${field.propertyName}s) {
        return orIn(V3${entity}.${field.name?upper_case}, V3${entity}.${field.name?upper_case}_COLUMN_NAME, CollUtil.newArrayList(${field.propertyName}s));
    }

    @JsonIgnore
    public V3${entity}Criteria set${field.propertyName?cap_first}NotIn(${field.propertyType}... ${field.propertyName}s) {
        return notIn(V3${entity}.${field.name?upper_case}, V3${entity}.${field.name?upper_case}_COLUMN_NAME, CollUtil.newArrayList(${field.propertyName}s));
    }

    @JsonIgnore
    public V3${entity}Criteria setOr${field.propertyName?cap_first}NotIn(${field.propertyType}... ${field.propertyName}s) {
        return orNotIn(V3${entity}.${field.name?upper_case}, V3${entity}.${field.name?upper_case}_COLUMN_NAME, CollUtil.newArrayList(${field.propertyName}s));
    }

    // endregion

