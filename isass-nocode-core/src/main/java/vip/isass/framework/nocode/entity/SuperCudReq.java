// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.entity;

import vip.isass.framework.nocode.criteria.IUpdateCriteria;
import vip.isass.framework.nocode.property.PropertyGetter;
import vip.isass.framework.nocode.property.PropertyNameResolver;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A composable create/update/delete change set executed by {@code superCud} in one transaction.
 * Every group is optional and multiple groups may be submitted together.
 */
public record SuperCudReq<E, C>(
        List<E> addEntities,
        List<String> addByFields,
        List<E> updateEntities,
        C updateCriteria,
        List<Serializable> deleteIds,
        List<C> deleteCriteria
) {

    public SuperCudReq {
        addEntities = immutableList(addEntities);
        addByFields = normalizedFields(addByFields);
        updateEntities = immutableList(updateEntities);
        deleteIds = immutableList(deleteIds);
        deleteCriteria = immutableList(deleteCriteria);
    }

    public static <E, C> SuperCudReq<E, C> empty() {
        return new SuperCudReq<>(null, null, null, null, null, null);
    }

    public static <E, C> SuperCudReq<E, C> add(E entity) {
        return new SuperCudReq<>(List.of(entity), null, null, null, null, null);
    }

    public static <E, C> SuperCudReq<E, C> addAll(Collection<E> entities) {
        return new SuperCudReq<>(copyOf(entities), null, null, null, null, null);
    }

    public static <E, C> SuperCudReq<E, C> addIfAbsent(E entity, String... fields) {
        return new SuperCudReq<>(List.of(entity), List.of(fields), null, null, null, null);
    }

    @SafeVarargs
    public static <E, C extends IUpdateCriteria<C>> SuperCudReq<E, C> addIfAbsent(
            E entity, PropertyGetter<E, ?> first, PropertyGetter<E, ?>... remaining) {
        return SuperCudReq.<E, C>builder().addEntity(entity).addByFields(first, remaining).build();
    }

    public static <E, C> SuperCudReq<E, C> update(E entity) {
        return new SuperCudReq<>(null, null, List.of(entity), null, null, null);
    }

    public static <E, C> SuperCudReq<E, C> updateAll(Collection<E> entities) {
        return new SuperCudReq<>(null, null, copyOf(entities), null, null, null);
    }

    public static <E, C> SuperCudReq<E, C> updateByCriteria(Collection<E> entities, C criteria) {
        return new SuperCudReq<>(null, null, copyOf(entities), criteria, null, null);
    }

    public static <E, C> SuperCudReq<E, C> delete(Serializable id) {
        return new SuperCudReq<>(null, null, null, null, List.of(id), null);
    }

    public static <E, C> SuperCudReq<E, C> deleteAll(Collection<? extends Serializable> ids) {
        return new SuperCudReq<>(null, null, null, null, copyOf(ids), null);
    }

    public static <E, C> SuperCudReq<E, C> deleteByCriteria(C criteria) {
        return new SuperCudReq<>(null, null, null, null, null, List.of(criteria));
    }

    public static <E, C extends IUpdateCriteria<C>> Builder<E, C> builder() {
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

    public static final class Builder<E, C extends IUpdateCriteria<C>> {

        private final List<E> addEntities = new ArrayList<>();
        private final List<E> updateEntities = new ArrayList<>();
        private final List<Serializable> deleteIds = new ArrayList<>();
        private final List<C> deleteCriteria = new ArrayList<>();
        private List<String> addByFields = List.of();
        private C updateCriteria;

        public Builder<E, C> addEntity(E entity) {
            addEntities.add(entity);
            return this;
        }

        public Builder<E, C> addEntities(Collection<? extends E> entities) {
            if (entities != null) addEntities.addAll(entities);
            return this;
        }

        public Builder<E, C> addByFields(String... fields) {
            addByFields = normalizedFields(fields == null ? null : List.of(fields));
            return this;
        }

        @SafeVarargs
        public final Builder<E, C> addByFields(
                PropertyGetter<E, ?> first, PropertyGetter<E, ?>... remaining) {
            addByFields = propertyNames(first, remaining);
            return this;
        }

        public Builder<E, C> updateEntity(E entity) {
            updateEntities.add(entity);
            return this;
        }

        public Builder<E, C> updateEntities(Collection<? extends E> entities) {
            if (entities != null) updateEntities.addAll(entities);
            return this;
        }

        public Builder<E, C> updateCriteria(C criteria) {
            updateCriteria = criteria;
            return this;
        }

        public Builder<E, C> updateByCriteria(C criteria) {
            return updateCriteria(criteria);
        }

        @SafeVarargs
        public final Builder<E, C> updateByCriteria(
                C criteria, PropertyGetter<E, ?> first, PropertyGetter<E, ?>... remaining) {
            criteria.setMatchFields(propertyNames(first, remaining));
            return updateCriteria(criteria);
        }

        public Builder<E, C> deleteId(Serializable id) {
            deleteIds.add(id);
            return this;
        }

        public Builder<E, C> deleteIds(Collection<? extends Serializable> ids) {
            if (ids != null) deleteIds.addAll(ids);
            return this;
        }

        public Builder<E, C> deleteByCriteria(C criteria) {
            deleteCriteria.add(criteria);
            return this;
        }

        public SuperCudReq<E, C> build() {
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
