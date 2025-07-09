<#if field.propertyType == "String">
    // region 字符串类型字段拥有的条件

    public V2${entity}Criteria set${field.propertyName?cap_first}Like(${field.propertyType} ${field.propertyName}Like) {
        return like(V2${entity}.${field.name?upper_case}, V2${entity}.${field.name?upper_case}_COLUMN_NAME, ${field.propertyName}Like);
    }

    public V2${entity}Criteria setOr${field.propertyName?cap_first}Like(${field.propertyType} or${field.propertyName?cap_first}Like) {
        return orLike(V2${entity}.${field.name?upper_case}, V2${entity}.${field.name?upper_case}_COLUMN_NAME, or${field.propertyName?cap_first}Like);
    }

    public V2${entity}Criteria set${field.propertyName?cap_first}NotLike(${field.propertyType} ${field.propertyName}NotLike) {
        return notLike(V2${entity}.${field.name?upper_case}, V2${entity}.${field.name?upper_case}_COLUMN_NAME, ${field.propertyName}NotLike);
    }

    public V2${entity}Criteria setOr${field.propertyName?cap_first}NotLike(${field.propertyType} or${field.propertyName?cap_first}NotLike) {
        return orNotLike(V2${entity}.${field.name?upper_case}, V2${entity}.${field.name?upper_case}_COLUMN_NAME, or${field.propertyName?cap_first}NotLike);
    }

    public V2${entity}Criteria set${field.propertyName?cap_first}StartWith(${field.propertyType} ${field.propertyName}StartWith) {
        return startWith(V2${entity}.${field.name?upper_case}, V2${entity}.${field.name?upper_case}_COLUMN_NAME, ${field.propertyName}StartWith);
    }

    public V2${entity}Criteria setOr${field.propertyName?cap_first}StartWith(${field.propertyType} or${field.propertyName?cap_first}StartWith) {
        return orStartWith(V2${entity}.${field.name?upper_case}, V2${entity}.${field.name?upper_case}_COLUMN_NAME, or${field.propertyName?cap_first}StartWith);
    }

    // endregion

</#if>