    // region 所有类型都有的条件

    public ${entity}Criteria set${field.propertyName?cap_first}(${field.propertyType} ${field.propertyName}) {
        return isConditionValuePresent(${field.propertyName}) ? equals(propertyName(${entity}::get${field.propertyName?cap_first}), ${field.propertyName}) : this;
    }

    public ${entity}Criteria setOr${field.propertyName?cap_first}(${field.propertyType} or${field.propertyName?cap_first}) {
        return isConditionValuePresent(or${field.propertyName?cap_first}) ? orEquals(propertyName(${entity}::get${field.propertyName?cap_first}), or${field.propertyName?cap_first}) : this;
    }

    public ${entity}Criteria set${field.propertyName?cap_first}NotEqual(${field.propertyType} ${field.propertyName}NotEqual) {
        return isConditionValuePresent(${field.propertyName}NotEqual) ? notEquals(propertyName(${entity}::get${field.propertyName?cap_first}), ${field.propertyName}NotEqual) : this;
    }

    public ${entity}Criteria setOr${field.propertyName?cap_first}NotEqual(${field.propertyType} or${field.propertyName?cap_first}NotEqual) {
        return isConditionValuePresent(or${field.propertyName?cap_first}NotEqual) ? orNotEquals(propertyName(${entity}::get${field.propertyName?cap_first}), or${field.propertyName?cap_first}NotEqual) : this;
    }

    public ${entity}Criteria set${field.propertyName?cap_first}In(Collection<${field.propertyType}> ${field.propertyName}s) {
        return isConditionValuePresent(${field.propertyName}s) ? in(propertyName(${entity}::get${field.propertyName?cap_first}), ${field.propertyName}s) : this;
    }

    public ${entity}Criteria setOr${field.propertyName?cap_first}In(Collection<${field.propertyType}> ${field.propertyName}s) {
        return isConditionValuePresent(${field.propertyName}s) ? orIn(propertyName(${entity}::get${field.propertyName?cap_first}), ${field.propertyName}s) : this;
    }

    public ${entity}Criteria set${field.propertyName?cap_first}NotIn(Collection<${field.propertyType}> ${field.propertyName}s) {
        return isConditionValuePresent(${field.propertyName}s) ? notIn(propertyName(${entity}::get${field.propertyName?cap_first}), ${field.propertyName}s) : this;
    }

    public ${entity}Criteria setOr${field.propertyName?cap_first}NotIn(Collection<${field.propertyType}> ${field.propertyName}s) {
        return isConditionValuePresent(${field.propertyName}s) ? orNotIn(propertyName(${entity}::get${field.propertyName?cap_first}), ${field.propertyName}s) : this;
    }

    public ${entity}Criteria set${field.propertyName?cap_first}IsNull(Boolean enabled) {
        return Boolean.TRUE.equals(enabled)
                ? isNull(${entity}::get${field.propertyName?cap_first})
                : this;
    }

    public ${entity}Criteria set${field.propertyName?cap_first}IsNotNull(Boolean enabled) {
        return Boolean.TRUE.equals(enabled)
                ? isNotNull(${entity}::get${field.propertyName?cap_first})
                : this;
    }

    @JsonIgnore
    public ${entity}Criteria set${field.propertyName?cap_first}In(${field.propertyType}... ${field.propertyName}s) {
        return isConditionValuePresent(${field.propertyName}s) ? in(propertyName(${entity}::get${field.propertyName?cap_first}), CollUtil.newArrayList(${field.propertyName}s)) : this;
    }

    @JsonIgnore
    public ${entity}Criteria setOr${field.propertyName?cap_first}In(${field.propertyType}... ${field.propertyName}s) {
        return isConditionValuePresent(${field.propertyName}s) ? orIn(propertyName(${entity}::get${field.propertyName?cap_first}), CollUtil.newArrayList(${field.propertyName}s)) : this;
    }

    @JsonIgnore
    public ${entity}Criteria set${field.propertyName?cap_first}NotIn(${field.propertyType}... ${field.propertyName}s) {
        return isConditionValuePresent(${field.propertyName}s) ? notIn(propertyName(${entity}::get${field.propertyName?cap_first}), CollUtil.newArrayList(${field.propertyName}s)) : this;
    }

    @JsonIgnore
    public ${entity}Criteria setOr${field.propertyName?cap_first}NotIn(${field.propertyType}... ${field.propertyName}s) {
        return isConditionValuePresent(${field.propertyName}s) ? orNotIn(propertyName(${entity}::get${field.propertyName?cap_first}), CollUtil.newArrayList(${field.propertyName}s)) : this;
    }

    // endregion
