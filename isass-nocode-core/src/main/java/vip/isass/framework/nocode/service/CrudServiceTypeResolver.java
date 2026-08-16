// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.service;

import org.springframework.core.ResolvableType;

/** Resolves the entity type declared by a concrete local CRUD service. */
public final class CrudServiceTypeResolver {

    private CrudServiceTypeResolver() {
    }

    public static Class<?> resolveEntityClass(Class<?> serviceClass) {
        return resolve(serviceClass, 0, "实体");
    }

    public static Class<?> resolveCriteriaClass(Class<?> serviceClass) {
        return resolve(serviceClass, 1, "Criteria");
    }

    private static Class<?> resolve(Class<?> serviceClass, int genericIndex, String label) {
        Class<?> entityClass = ResolvableType.forClass(serviceClass)
                .as(ILocalCrudService.class)
                .getGeneric(genericIndex)
                .resolve();
        if (entityClass == null) {
            throw new IllegalStateException("无法解析 ILocalCrudService " + label + " 泛型: " + serviceClass.getName());
        }
        return entityClass;
    }
}
