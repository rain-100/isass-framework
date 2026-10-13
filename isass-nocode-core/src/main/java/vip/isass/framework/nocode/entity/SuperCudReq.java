// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.entity;

import vip.isass.framework.common.criteria.IUpdateCriteria;
import vip.isass.framework.common.property.PropertyGetter;
import vip.isass.framework.common.property.PropertyNameResolver;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A composable create/update/delete change set executed by {@code superCud} in one transaction.
 * Every group is optional and multiple groups may be submitted together.
 * PK describes ID-based deletion and is resolved before JSON deserialization.
 * The entity payload is not required to expose a primary key.
 */
public record SuperCudReq<E, C, PK extends Serializable>(
        List<E> addEntities,
        List<String> addByFields,
        List<E> updateEntities,
        C updateCriteria,
        List<PK> deleteIds,
        List<C> deleteCriteria
) {

    public SuperCudReq {
        addEntities = immutableList(addEntities);
        addByFields = normalizedFields(addByFields);
        updateEntities = immutableList(updateEntities);
        deleteIds = immutableList(deleteIds);
        deleteCriteria = immutableList(deleteCriteria);
    }

    public static <E, C, PK extends Serializable> SuperCudReq<E, C, PK> empty() {
        return new SuperCudReq<>(null, null, null, null, null, null);
    }

    public static <E, C, PK extends Serializable> SuperCudReq<E, C, PK> add(E entity) {
        return new SuperCudReq<>(List.of(entity), null, null, null, null, null);
    }

    public static <E, C, PK extends Serializable> SuperCudReq<E, C, PK> addAll(Collection<E> entities) {
        return new SuperCudReq<>(copyOf(entities), null, null, null, null, null);
    }

    public static <E, C, PK extends Serializable> SuperCudReq<E, C, PK> addIfAbsent(E entity, String... fields) {
        return new SuperCudReq<>(List.of(entity), List.of(fields), null, null, null, null);
    }

    @SafeVarargs
    public static <E, C extends IUpdateCriteria<C>, PK extends Serializable> SuperCudReq<E, C, PK> addIfAbsent(
            E entity, PropertyGetter<E, ?> first, PropertyGetter<E, ?>... remaining) {
        return SuperCudReq.<E, C, PK>builder().addEntity(entity).addByFields(first, remaining).build();
    }

    public static <E, C, PK extends Serializable> SuperCudReq<E, C, PK> update(E entity) {
        return new SuperCudReq<>(null, null, List.of(entity), null, null, null);
    }

    public static <E, C, PK extends Serializable> SuperCudReq<E, C, PK> updateAll(Collection<E> entities) {
        return new SuperCudReq<>(null, null, copyOf(entities), null, null, null);
    }

    public static <E, C, PK extends Serializable> SuperCudReq<E, C, PK> updateByCriteria(Collection<E> entities, C criteria) {
        return new SuperCudReq<>(null, null, copyOf(entities), criteria, null, null);
    }

    public static <E, C, PK extends Serializable> SuperCudReq<E, C, PK> delete(PK id) {
        return new SuperCudReq<>(null, null, null, null, List.of(id), null);
    }

    public static <E, C, PK extends Serializable> SuperCudReq<E, C, PK> deleteAll(Collection<? extends PK> ids) {
        return new SuperCudReq<>(null, null, null, null, copyOf(ids), null);
    }

    public static <E, C, PK extends Serializable> SuperCudReq<E, C, PK> deleteByCriteria(C criteria) {
        return new SuperCudReq<>(null, null, null, null, null, List.of(criteria));
    }

    public static <E, C extends IUpdateCriteria<C>, PK extends Serializable> Builder<E, C, PK> builder() {
        return new Builder<>();
    }

    public boolean isEmpty() {
        return addEntities.isEmpty()
                && updateEntities.isEmpty()
                && deleteIds.isEmpty()
                && deleteCriteria.isEmpty();
    }

    private static <T> List<T> immutableList(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private static <T> List<T> copyOf(Collection<? extends T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private static List<String> normalizedFields(Collection<String> fields) {
        if (fields == null) {
            return List.of();
        }
        return fields.stream()
                .filter(field -> field != null && !field.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    public static final class Builder<E, C extends IUpdateCriteria<C>, PK extends Serializable> {

        private final List<E> addEntities = new ArrayList<>();
        private final List<E> updateEntities = new ArrayList<>();
        private final List<PK> deleteIds = new ArrayList<>();
        private final List<C> deleteCriteria = new ArrayList<>();
        private List<String> addByFields = List.of();
        private C updateCriteria;

        public Builder<E, C, PK> addEntity(E entity) {
            addEntities.add(entity);
            return this;
        }

        public Builder<E, C, PK> addEntities(Collection<? extends E> entities) {
            if (entities != null) addEntities.addAll(entities);
            return this;
        }

        public Builder<E, C, PK> addByFields(String... fields) {
            addByFields = normalizedFields(fields == null ? null : List.of(fields));
            return this;
        }

        @SafeVarargs
        public final Builder<E, C, PK> addByFields(
                PropertyGetter<E, ?> first, PropertyGetter<E, ?>... remaining) {
            addByFields = propertyNames(first, remaining);
            return this;
        }

        public Builder<E, C, PK> updateEntity(E entity) {
            updateEntities.add(entity);
            return this;
        }

        public Builder<E, C, PK> updateEntities(Collection<? extends E> entities) {
            if (entities != null) updateEntities.addAll(entities);
            return this;
        }

        public Builder<E, C, PK> updateCriteria(C criteria) {
            updateCriteria = criteria;
            return this;
        }

        public Builder<E, C, PK> updateByCriteria(C criteria) {
            return updateCriteria(criteria);
        }

        @SafeVarargs
        public final Builder<E, C, PK> updateByCriteria(
                C criteria, PropertyGetter<E, ?> first, PropertyGetter<E, ?>... remaining) {
            criteria.setMatchFields(propertyNames(first, remaining));
            return updateCriteria(criteria);
        }

        public Builder<E, C, PK> deleteId(PK id) {
            deleteIds.add(id);
            return this;
        }

        public Builder<E, C, PK> deleteIds(Collection<? extends PK> ids) {
            if (ids != null) deleteIds.addAll(ids);
            return this;
        }

        public Builder<E, C, PK> deleteByCriteria(C criteria) {
            deleteCriteria.add(criteria);
            return this;
        }

        public SuperCudReq<E, C, PK> build() {
            return new SuperCudReq<>(addEntities, addByFields, updateEntities,
                    updateCriteria, deleteIds, deleteCriteria);
        }

        @SafeVarargs
        private static <E> List<String> propertyNames(
                PropertyGetter<E, ?> first, PropertyGetter<E, ?>... remaining) {
            List<String> result = new ArrayList<>(remaining.length + 1);
            result.add(PropertyNameResolver.resolve(first));
            for (PropertyGetter<E, ?> getter : remaining) {
                result.add(PropertyNameResolver.resolve(getter));
            }
            return normalizedFields(result);
        }
    }
}
