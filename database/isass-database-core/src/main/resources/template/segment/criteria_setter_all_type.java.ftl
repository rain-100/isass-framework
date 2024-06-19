    public ${entity}Criteria set${field.propertyName?cap_first}(${field.propertyType} ${field.propertyName}) {
        this.${field.propertyName} = ${field.propertyName};
        equals(${entity}.${field.name?upper_case}, this.${field.propertyName});
        return this;
    }

    public ${entity}Criteria setOr${field.propertyName?cap_first}(${field.propertyType} ${field.propertyName}) {
        this.or${field.propertyName?cap_first} = ${field.propertyName};
        orEquals(${entity}.${field.name?upper_case}, this.or${field.propertyName?cap_first});
        return this;
    }

    public ${entity}Criteria set${field.propertyName?cap_first}NotEqual(${field.propertyType} ${field.propertyName}NotEqual) {
        this.${field.propertyName}NotEqual = ${field.propertyName}NotEqual;
        notEquals(${entity}.${field.name?upper_case}, this.${field.propertyName}NotEqual);
        return this;
    }

    public ${entity}Criteria setOr${field.propertyName?cap_first}NotEqual(${field.propertyType} or${field.propertyName?cap_first}NotEqual) {
        this.or${field.propertyName?cap_first}NotEqual = or${field.propertyName?cap_first}NotEqual;
        orNotEquals(${entity}.${field.name?upper_case}, this.or${field.propertyName?cap_first}NotEqual);
        return this;
    }

    public ${entity}Criteria set${field.propertyName?cap_first}In(Collection<${field.propertyType}> ${field.propertyName}s) {
        this.${field.propertyName}In = ${field.propertyName}s;
        in(${entity}.${field.name?upper_case}, this.${field.propertyName}In);
        return this;
    }

    public ${entity}Criteria setOr${field.propertyName?cap_first}In(Collection<${field.propertyType}> ${field.propertyName}s) {
        this.or${field.propertyName?cap_first}In = ${field.propertyName}s;
        orIn(${entity}.${field.name?upper_case}, this.or${field.propertyName?cap_first}In);
        return this;
    }

    public ${entity}Criteria set${field.propertyName?cap_first}NotIn(Collection<${field.propertyType}> ${field.propertyName}s) {
        this.${field.propertyName}NotIn = ${field.propertyName}s;
        notIn(${entity}.${field.name?upper_case}, this.${field.propertyName}NotIn);
        return this;
    }

    public ${entity}Criteria setOr${field.propertyName?cap_first}NotIn(Collection<${field.propertyType}> ${field.propertyName}s) {
        this.or${field.propertyName?cap_first}NotIn = ${field.propertyName}s;
        orNotIn(${entity}.${field.name?upper_case}, this.or${field.propertyName?cap_first}NotIn);
        return this;
    }

    @JsonIgnore
    public ${entity}Criteria set${field.propertyName?cap_first}In(${field.propertyType}... ${field.propertyName}s) {
        this.${field.propertyName}In = CollUtil.newHashSet(${field.propertyName}s);
        in(${entity}.${field.name?upper_case}, this.${field.propertyName}In);
        return this;
    }

    @JsonIgnore
    public ${entity}Criteria setOr${field.propertyName?cap_first}In(${field.propertyType}... ${field.propertyName}s) {
        this.or${field.propertyName?cap_first}In = CollUtil.newHashSet(${field.propertyName}s);
        orIn(${entity}.${field.name?upper_case}, this.or${field.propertyName?cap_first}In);
        return this;
    }

    @JsonIgnore
    public ${entity}Criteria set${field.propertyName?cap_first}NotIn(${field.propertyType}... ${field.propertyName}s) {
        this.${field.propertyName}NotIn = CollUtil.newHashSet(${field.propertyName}s);
        notIn(${entity}.${field.name?upper_case}, this.${field.propertyName}NotIn);
        return this;
    }

    @JsonIgnore
    public ${entity}Criteria setOr${field.propertyName?cap_first}NotIn(${field.propertyType}... ${field.propertyName}s) {
        this.or${field.propertyName?cap_first}NotIn = CollUtil.newHashSet(${field.propertyName}s);
        orNotIn(${entity}.${field.name?upper_case}, this.or${field.propertyName?cap_first}NotIn);
        return this;
    }

