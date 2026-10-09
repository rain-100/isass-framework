// SPDX-License-Identifier: LGPL-3.0-only
package vip.isass.framework.nocode.generator.model;

import java.util.List;
import java.util.Set;

/**
 * 实体的业务语义声明；不复制表名、列名或数据库类型。
 */
public record EntityModelDefinition(String entityName, String domain, String subdomain,
                                    boolean tenantIsolation, List<EntityFieldDefinition> fields) {
    public EntityModelDefinition {
        if (entityName == null || !entityName.matches("[A-Z][a-zA-Z0-9]*")) {
            throw new IllegalArgumentException("非法实体名: " + entityName);
        }
        if (domain == null || !domain.matches("[a-z][a-z0-9]*")
                || subdomain != null && (!subdomain.matches("[a-z][a-z0-9]*")
                || Set.of("application", "domain", "infrastructure", "interfaces").contains(subdomain))) {
            throw new IllegalArgumentException("非法领域包归属: " + entityName);
        }
        fields = List.copyOf(fields);
        if (fields.stream().map(EntityFieldDefinition::propertyName).distinct().count() != fields.size()) {
            throw new IllegalArgumentException("字段声明重复: " + entityName);
        }
    }

    public static EntityModelDefinition of(String entity, String domain) {
        return of(entity, domain, null);
    }

    public static EntityModelDefinition of(String entity, String domain, String subdomain) {
        return new EntityModelDefinition(entity, domain, subdomain, true, List.of());
    }

    public EntityModelDefinition withFields(EntityFieldDefinition... definitions) {
        return new EntityModelDefinition(entityName, domain, subdomain, tenantIsolation, List.of(definitions));
    }

    public EntityModelDefinition withoutTenantIsolation() {
        return new EntityModelDefinition(entityName, domain, subdomain, false, fields);
    }
}
