<#if field.propertyType == "String">
    // region 字符串类型字段拥有的条件

    public ${entity}Criteria set${field.propertyName?cap_first}Like(${field.propertyType} ${field.propertyName}Like) {
        return like(${entity}.${field.name?upper_case}, ${entity}.${field.name?upper_case}_COLUMN_NAME, ${field.propertyName}Like);
    }

    public ${entity}Criteria setOr${field.propertyName?cap_first}Like(${field.propertyType} or${field.propertyName?cap_first}Like) {
        return orLike(${entity}.${field.name?upper_case}, ${entity}.${field.name?upper_case}_COLUMN_NAME, or${field.propertyName?cap_first}Like);
    }

    public ${entity}Criteria set${field.propertyName?cap_first}NotLike(${field.propertyType} ${field.propertyName}NotLike) {
        return notLike(${entity}.${field.name?upper_case}, ${entity}.${field.name?upper_case}_COLUMN_NAME, ${field.propertyName}NotLike);
    }

    public ${entity}Criteria setOr${field.propertyName?cap_first}NotLike(${field.propertyType} or${field.propertyName?cap_first}NotLike) {
        return orNotLike(${entity}.${field.name?upper_case}, ${entity}.${field.name?upper_case}_COLUMN_NAME, or${field.propertyName?cap_first}NotLike);
    }

    public ${entity}Criteria set${field.propertyName?cap_first}StartWith(${field.propertyType} ${field.propertyName}StartWith) {
        return startWith(${entity}.${field.name?upper_case}, ${entity}.${field.name?upper_case}_COLUMN_NAME, ${field.propertyName}StartWith);
    }

    public ${entity}Criteria setOr${field.propertyName?cap_first}StartWith(${field.propertyType} or${field.propertyName?cap_first}StartWith) {
        return orStartWith(${entity}.${field.name?upper_case}, ${entity}.${field.name?upper_case}_COLUMN_NAME, or${field.propertyName?cap_first}StartWith);
    }

    // endregion

</#if>