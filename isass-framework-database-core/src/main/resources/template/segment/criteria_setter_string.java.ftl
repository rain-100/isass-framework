<#if field.propertyType == "String">
    public ${entity}Criteria set${field.propertyName?cap_first}Like(${field.propertyType} ${field.propertyName}Like) {
        this.${field.propertyName}Like = ${field.propertyName}Like == null ? null : ${field.propertyName}Like.trim();
        like(${entity}.${field.name?upper_case}, this.${field.propertyName}Like);
        return this;
    }

    public ${entity}Criteria setOr${field.propertyName?cap_first}Like(${field.propertyType} or${field.propertyName?cap_first}Like) {
        this.or${field.propertyName?cap_first}Like = or${field.propertyName?cap_first}Like == null ? null : or${field.propertyName?cap_first}Like.trim();
        orLike(${entity}.${field.name?upper_case}, this.or${field.propertyName?cap_first}Like);
        return this;
    }

    public ${entity}Criteria set${field.propertyName?cap_first}NotLike(${field.propertyType} ${field.propertyName}NotLike) {
        this.${field.propertyName}NotLike = ${field.propertyName}NotLike == null ? null : ${field.propertyName}NotLike.trim();
        notLike(${entity}.${field.name?upper_case}, this.${field.propertyName}NotLike);
        return this;
    }

    public ${entity}Criteria setOr${field.propertyName?cap_first}NotLike(${field.propertyType} or${field.propertyName?cap_first}NotLike) {
        this.or${field.propertyName?cap_first}NotLike = or${field.propertyName?cap_first}NotLike == null ? null : or${field.propertyName?cap_first}NotLike.trim();
        orNotLike(${entity}.${field.name?upper_case}, this.or${field.propertyName?cap_first}NotLike);
        return this;
    }

    public ${entity}Criteria set${field.propertyName?cap_first}StartWith(${field.propertyType} ${field.propertyName}StartWith) {
        this.${field.propertyName}StartWith = ${field.propertyName}StartWith == null ? null : ${field.propertyName}StartWith.trim();
        startWith(${entity}.${field.name?upper_case}, ${field.propertyName}StartWith);
        return this;
    }

    public ${entity}Criteria setOr${field.propertyName?cap_first}StartWith(${field.propertyType} or${field.propertyName?cap_first}StartWith) {
        this.or${field.propertyName?cap_first}StartWith = or${field.propertyName?cap_first}StartWith == null ? null : or${field.propertyName?cap_first}StartWith.trim();
        orStartWith(${entity}.${field.name?upper_case}, or${field.propertyName?cap_first}StartWith);
        return this;
    }

</#if>