<#if field.propertyType == "String">${"\n"}    // region 字符串类型字段拥有的条件

    public ${entity}Criteria set${field.propertyName?cap_first}Like(${field.propertyType} ${field.propertyName}Like) {
        return isConditionValuePresent(${field.propertyName}Like) ? like(propertyName(${entity}::get${field.propertyName?cap_first}), ${field.propertyName}Like) : this;
    }

    public ${entity}Criteria setOr${field.propertyName?cap_first}Like(${field.propertyType} or${field.propertyName?cap_first}Like) {
        return isConditionValuePresent(or${field.propertyName?cap_first}Like) ? orLike(propertyName(${entity}::get${field.propertyName?cap_first}), or${field.propertyName?cap_first}Like) : this;
    }

    public ${entity}Criteria set${field.propertyName?cap_first}NotLike(${field.propertyType} ${field.propertyName}NotLike) {
        return isConditionValuePresent(${field.propertyName}NotLike) ? notLike(propertyName(${entity}::get${field.propertyName?cap_first}), ${field.propertyName}NotLike) : this;
    }

    public ${entity}Criteria setOr${field.propertyName?cap_first}NotLike(${field.propertyType} or${field.propertyName?cap_first}NotLike) {
        return isConditionValuePresent(or${field.propertyName?cap_first}NotLike) ? orNotLike(propertyName(${entity}::get${field.propertyName?cap_first}), or${field.propertyName?cap_first}NotLike) : this;
    }

    public ${entity}Criteria set${field.propertyName?cap_first}StartWith(${field.propertyType} ${field.propertyName}StartWith) {
        return isConditionValuePresent(${field.propertyName}StartWith) ? startWith(propertyName(${entity}::get${field.propertyName?cap_first}), ${field.propertyName}StartWith) : this;
    }

    public ${entity}Criteria setOr${field.propertyName?cap_first}StartWith(${field.propertyType} or${field.propertyName?cap_first}StartWith) {
        return isConditionValuePresent(or${field.propertyName?cap_first}StartWith) ? orStartWith(propertyName(${entity}::get${field.propertyName?cap_first}), or${field.propertyName?cap_first}StartWith) : this;
    }

    public ${entity}Criteria set${field.propertyName?cap_first}IsEmpty(Boolean enabled) {
        return Boolean.TRUE.equals(enabled)
                ? isEmpty(${entity}::get${field.propertyName?cap_first})
                : this;
    }

    public ${entity}Criteria set${field.propertyName?cap_first}IsNotEmpty(Boolean enabled) {
        return Boolean.TRUE.equals(enabled)
                ? isNotEmpty(${entity}::get${field.propertyName?cap_first})
                : this;
    }

    // endregion

</#if>
