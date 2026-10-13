// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.service;

import vip.isass.framework.common.criteria.type.IWhereConditionCriteria;
import vip.isass.framework.common.entity.IIdEntity;

import java.io.Serializable;
import java.util.Collection;

/** ID operations are an optional entity capability, not a CRUD generic bound. */
final class CrudIdSupport {

    private CrudIdSupport() {
    }

    static boolean supportsId(ICrudService<?, ?, ?> service) {
        return IIdEntity.class.isAssignableFrom(CrudServiceTypeResolver.resolveEntityClass(service.getClass()));
    }

    static void requireIdEntity(ICrudService<?, ?, ?> service, String operation) {
        requireIdEntityType(CrudServiceTypeResolver.resolveEntityClass(service.getClass()), operation);
    }

    static void requireIdEntityType(Class<?> entityType, String operation) {
        if (!IIdEntity.class.isAssignableFrom(entityType)) {
            throw new UnsupportedOperationException(operation + " 要求实体实现 IIdEntity: " + entityType.getName());
        }
    }

    static IIdEntity<?, ?> requireIdEntity(Object entity, String operation) {
        if (!(entity instanceof IIdEntity<?, ?> identified)) {
            throw new UnsupportedOperationException(operation + " 要求实体实现 IIdEntity");
        }
        return identified;
    }

    static Serializable requireId(Object entity, String operation) {
        Serializable id = requireIdEntity(entity, operation).getId();
        requireIdValue(id, operation);
        return id;
    }

    static void requireIdValue(Serializable id, String operation) {
        if (id == null || id instanceof CharSequence value && value.toString().isBlank()) {
            throw new IllegalArgumentException(operation + " 的 ID 不能为空");
        }
    }

    static IWhereConditionCriteria<?, ?> where(Object criteria) {
        if (!(criteria instanceof IWhereConditionCriteria<?, ?> where)) {
            throw new UnsupportedOperationException("ID 操作的 Criteria 必须实现 IWhereConditionCriteria");
        }
        return where;
    }

    static <C> C equalsId(ICrudService<?, ?, ?> service, C criteria, Serializable id) {
        requireIdEntity(service, "按 ID 操作");
        requireIdValue(id, "按 ID 操作");
        where(criteria).equals("id", id);
        return criteria;
    }

    static <C> C inIds(ICrudService<?, ?, ?> service, C criteria, Collection<? extends Serializable> ids) {
        requireIdEntity(service, "deleteIds");
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("deleteIds 不能为空");
        }
        ids.forEach(id -> requireIdValue(id, "deleteIds"));
        where(criteria).in("id", ids);
        return criteria;
    }
}
