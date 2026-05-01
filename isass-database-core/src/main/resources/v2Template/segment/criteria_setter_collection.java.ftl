<#if field.propertyType?starts_with("Collection<")>
    // region 集合类型字段拥有的条件

    public ${entity}Criteria set${field.propertyName?cap_first}ContainsAll(${field.propertyType} ${field.propertyName}ContainsAll) {
        return collectionContainsAll(V2${entity}.${field.name?upper_case}, V2${entity}.${field.name?upper_case}_COLUMN_NAME, ${field.propertyName}ContainsAll);
    }

    public ${entity}Criteria set${field.propertyName?cap_first}ContainsAny(${field.propertyType} ${field.propertyName}ContainsAny) {
        return collectionContainsAny(V2${entity}.${field.name?upper_case}, V2${entity}.${field.name?upper_case}_COLUMN_NAME, ${field.propertyName}ContainsAny);
    }

    // endregion

</#if>