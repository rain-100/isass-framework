<#if field.propertyType?starts_with("Collection<")>
    // region 集合类型字段拥有的条件

    public ${entity}Criteria set${field.propertyName?cap_first}ContainsAll(${field.propertyType} ${field.propertyName}ContainsAll) {
        return isConditionValuePresent(${field.propertyName}ContainsAll)
                ? collectionContainsAll(propertyName(${entity}::get${field.propertyName?cap_first}), ${field.propertyName}ContainsAll)
                : this;
    }

    public ${entity}Criteria set${field.propertyName?cap_first}ContainsAny(${field.propertyType} ${field.propertyName}ContainsAny) {
        return isConditionValuePresent(${field.propertyName}ContainsAny)
                ? collectionContainsAny(propertyName(${entity}::get${field.propertyName?cap_first}), ${field.propertyName}ContainsAny)
                : this;
    }

    // endregion

</#if>
