<#if field.propertyType == "Collection<String>">
    public ${entity}Criteria set${field.propertyName?cap_first}ContainsAll(Collection<String> ${field.propertyName}ContainsAll) {
        this.${field.propertyName}ContainsAll = ${field.propertyName}ContainsAll;
        collectionContainsAll(${entity}.${field.name?upper_case}, this.${field.propertyName}ContainsAll);
        return this;
    }

    public ${entity}Criteria set${field.propertyName?cap_first}ContainsAny(Collection<String> ${field.propertyName}ContainsAny) {
        this.${field.propertyName}ContainsAny = ${field.propertyName}ContainsAny;
        collectionContainsAny(${entity}.${field.name?upper_case}, this.${field.propertyName}ContainsAny);
        return this;
    }

</#if>