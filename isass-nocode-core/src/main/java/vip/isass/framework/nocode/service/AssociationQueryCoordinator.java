// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.service;

import vip.isass.framework.common.criteria.ICriteria;
import vip.isass.framework.common.criteria.IRelatedQueryCriteria;
import vip.isass.framework.common.criteria.CriteriaEntityTypes;
import vip.isass.framework.common.criteria.CriteriaMetadata;
import vip.isass.framework.common.criteria.EmptyCriteria;
import vip.isass.framework.common.criteria.RelatedCondition;
import vip.isass.framework.common.criteria.JoinCondition;
import vip.isass.framework.common.criteria.JoinResultPropertyResolver;
import vip.isass.framework.common.criteria.WhereCondition;
import vip.isass.framework.common.criteria.impl.type.Condition;
import vip.isass.framework.common.criteria.type.IPageCriteria;
import vip.isass.framework.common.criteria.type.IReturnFieldCriteria;
import vip.isass.framework.common.criteria.type.IOrderByCriteria;
import vip.isass.framework.common.entity.IIdEntity;
import vip.isass.framework.common.page.Page;
import vip.isass.framework.common.criteria.type.IWhereConditionCriteria;
import vip.isass.framework.common.entity.EntityAssociation;
import vip.isass.framework.common.entity.IEntity;

import java.beans.Introspector;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Objects;

/** Loads related entities in batches through their normal query lifecycle. */
public final class AssociationQueryCoordinator {

    private static final int MAX_ASSOCIATION_DEPTH = 16;

    private static final int BATCH_SIZE = 1000;
    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);
    private static final ClassValue<Map<String, PropertyDescriptor>> PROPERTIES = new ClassValue<>() {
        @Override
        protected Map<String, PropertyDescriptor> computeValue(Class<?> type) {
            try {
                Map<String, PropertyDescriptor> properties = new LinkedHashMap<>();
                for (PropertyDescriptor property : Introspector.getBeanInfo(type).getPropertyDescriptors()) {
                    properties.put(property.getName(), property);
                }
                return Map.copyOf(properties);
            } catch (IntrospectionException exception) {
                throw new IllegalStateException("无法分析实体属性: " + type.getName(), exception);
            }
        }
    };
    private final Map<Class<?>, ILocalCrudService<?, ?, ?>> services;

    public AssociationQueryCoordinator(Collection<ILocalCrudService<?, ?, ?>> localServices) {
        Map<Class<?>, ILocalCrudService<?, ?, ?>> indexed = new LinkedHashMap<>();
        for (ILocalCrudService<?, ?, ?> service : localServices) {
            Class<?> entityType = CrudServiceTypeResolver.resolveEntityClass(service.getClass());
            CriteriaEntityTypes.register(CrudServiceTypeResolver.resolveCriteriaClass(service.getClass()));
            if (indexed.putIfAbsent(entityType, service) != null) {
                throw new IllegalStateException("实体存在多个 ILocalCrudService: " + entityType.getName());
            }
        }
        services = Map.copyOf(indexed);
    }

    /** 直接在本次查询 Criteria 中补齐关系键及结果属性。 */
    public void prepare(ICriteria<?, ?> criteria) {
        if (!(criteria instanceof IRelatedQueryCriteria<?, ?> related)) return;
        Class<?> entityType = CriteriaEntityTypes.entityClass(criteria);
        Set<String> properties = new LinkedHashSet<>();
        for (RelatedCondition condition : related.getLoadRelated()) {
            if (!properties.add(condition.getProperty())) throw new IllegalArgumentException("重复 loadRelated 属性: " + condition.getProperty());
            validateNestedPage(condition.getCriteria());
            EntityAssociation relation = requireAssociation(entityType, condition);
            if (criteria instanceof IReturnFieldCriteria<?, ?> selected && !selected.getReturnFields().isEmpty()) {
                selected.addReturnField(relation.localField());
            }
        }
        for (JoinCondition join : related.getJoinConditions()) {
            String property = JoinResultPropertyResolver.resolve(entityType, join).property();
            if (!properties.add(property)) {
                throw new IllegalArgumentException("关联属性被重复装配: " + property);
            }
            join.setResultProperty(property);
            prepare(join.getTargetCriteria());
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public <E extends IEntity<E>> List<E> populate(List<E> records, Object criteria) {
        if (records == null || records.isEmpty() || !(criteria instanceof IRelatedQueryCriteria<?, ?> related)
                || related.getLoadRelated().isEmpty() && related.getJoinConditions().isEmpty()) return records;
        int depth = DEPTH.get();
        if (depth >= MAX_ASSOCIATION_DEPTH) throw new IllegalArgumentException("loadRelated 超过最大深度 16");
        DEPTH.set(depth + 1);
        try {
            Set<String> properties = new LinkedHashSet<>();
            for (RelatedCondition condition : related.getLoadRelated()) {
                if (!properties.add(condition.getProperty())) throw new IllegalArgumentException("重复 loadRelated 属性: " + condition.getProperty());
                populate(records, requireAssociation(records.getFirst().getClass(), condition), condition);
            }
            for (JoinCondition join : related.getJoinConditions()) {
                if (join.getResultProperty() == null) {
                    throw new IllegalStateException("JOIN 缺少执行前准备的 resultProperty");
                }
                List targets = new ArrayList();
                for (E record : records) {
                    Object target = read(record, join.getResultProperty());
                    if (target instanceof Collection<?> values) {
                        targets.addAll(values);
                    } else if (target != null) {
                        targets.add(target);
                    }
                }
                populate(targets, join.getTargetCriteria());
            }
            return records;
        } finally {
            if (depth == 0) DEPTH.remove();
            else DEPTH.set(depth);
        }
    }

    private EntityAssociation requireAssociation(Class<?> source, RelatedCondition condition) {
        EntityAssociation association = association(source, condition.getProperty());
        if (association == null) throw new IllegalArgumentException("未声明的关联属性: " + condition.getProperty());
        if (condition.getLocalKey() != null && !condition.getLocalKey().equals(association.localField())
                || condition.getTargetKey() != null && !condition.getTargetKey().equals(association.targetField())) {
            throw new IllegalArgumentException("loadRelated 关联键与实体元数据不一致: " + condition.getProperty());
        }
        if (condition.getCriteria() != null && CriteriaEntityTypes.entityClass(condition.getCriteria()) != association.targetType()) {
            throw new IllegalArgumentException("loadRelated 目标 Criteria 类型不一致: " + condition.getProperty());
        }
        condition.setLocalKey(association.localField()).setTargetKey(association.targetField());
        return association;
    }

    private EntityAssociation association(Class<?> source, String property) {
        if (!IEntity.class.isAssignableFrom(source)) {
            throw new IllegalStateException("关联源对象未实现 IEntity: " + source.getName());
        }
        return CriteriaMetadata.associations(source).stream()
                .filter(candidate -> candidate.property().equals(property))
                .findFirst()
                .orElse(null);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void populate(List<?> sources, EntityAssociation association, RelatedCondition related) {
        ILocalCrudService targetService = services.get(association.targetType());
        if (targetService == null) throw new IllegalStateException("关联目标没有本地 ILocalCrudService: " + association.targetType().getName());
        Set<Object> keys = new LinkedHashSet<>();
        sources.forEach(source -> {
            Object value = read(source, association.localField());
            if (value != null) keys.add(value);
        });
        List<Object> keyList = new ArrayList<>(keys);
        Map<Object, Map<Object, Object>> grouped = new LinkedHashMap<>();
        if (!keyList.isEmpty()) {
            ICriteria criteria = related.getCriteria();
            if (criteria == null || criteria instanceof EmptyCriteria<?>) {
                criteria = targetService.newCriteria();
                related.setCriteria(criteria);
            }
            validateNestedPage(criteria);
            if (!(criteria instanceof IWhereConditionCriteria where)) throw new IllegalStateException("关联目标 Criteria 不支持 WHERE");
            if (criteria instanceof IReturnFieldCriteria selected && !selected.getReturnFields().isEmpty()) {
                selected.addReturnField("id");
                selected.addReturnField(association.targetField());
            }
            IPageCriteria page = (IPageCriteria) criteria;
            Long originalPageNum = page.getPageNum();
            Long originalPageSize = page.getPageSize();
            Boolean originalSearchCount = page.getSearchCountFlag();
            ((IOrderByCriteria) criteria).orderByIfBlank("id", "asc");
            List<WhereCondition> original = where.getWhereConditions();
            WhereCondition businessGroup = new WhereCondition().setChildren(original);
            List<WhereCondition> conditions = new ArrayList<>();
            if (!original.isEmpty()) conditions.add(businessGroup);
            WhereCondition relationIn = new WhereCondition(association.targetField(), Condition.Membership.IN, null);
            conditions.add(relationIn);
            where.setWhereConditions(conditions);
            try {
                for (int offset = 0; offset < keyList.size(); offset += BATCH_SIZE) {
                    // 只替换框架持有的 IN 节点，不覆盖业务同字段筛选，不累积前一批 ID。
                    relationIn.setValue(keyList.subList(offset, Math.min(offset + BATCH_SIZE, keyList.size())));
                    page.setPageSize((long) BATCH_SIZE);
                    page.setSearchCountFlag(false);
                    for (long pageNum = 1; ; pageNum++) {
                        page.setPageNum(pageNum);
                        Page<?> result = targetService.page(criteria);
                        List<?> targets = result.getRecords();
                        for (Object target : targets) {
                            Object id = ((IIdEntity<?, ?>) target).getId();
                            if (id == null) throw new IllegalStateException("loadRelated 目标缺少 id: " + association.property());
                            grouped.computeIfAbsent(read(target, association.targetField()), ignored -> new LinkedHashMap<>()).putIfAbsent(id, target);
                        }
                        // ORM 可缩小页大小，按实际页大小继续查询，避免截断关联记录。
                        if (targets.isEmpty() || targets.size() < result.getPageSize()) break;
                        page.setPageSize(result.getPageSize());
                    }
                }
            } finally {
                // 临时批次条件仅在本次加载期间有效；其余生命周期修改不回滚。
                conditions.removeIf(condition -> condition == relationIn);
                if (where.getWhereConditions() == conditions && (conditions.isEmpty()
                        || conditions.size() == 1 && conditions.getFirst() == businessGroup)) {
                    where.setWhereConditions(original);
                }
                page.setPageNum(originalPageNum);
                page.setPageSize(originalPageSize);
                page.setSearchCountFlag(originalSearchCount);
            }
        }
        boolean many = Collection.class.isAssignableFrom(descriptor(sources.getFirst().getClass(), association.property()).getPropertyType());
        for (Object source : sources) {
            List<Object> matches = new ArrayList<>(grouped.getOrDefault(read(source, association.localField()), Map.of()).values());
            if (!many && matches.size() > 1) throw new IllegalStateException("单体关联返回多个不同目标: " + association.property());
            write(source, association.property(), many ? matches : matches.isEmpty() ? null : matches.getFirst());
        }
    }

    private static void validateNestedPage(ICriteria<?, ?> criteria) {
        if (criteria instanceof IPageCriteria<?, ?> page
                && (!Objects.equals(page.getPageNum(), IPageCriteria.DEFAULT_PAGE_NUM)
                || !Objects.equals(page.getPageSize(), IPageCriteria.DEFAULT_PAGE_SIZE)
                || !Objects.equals(page.getSearchCountFlag(), IPageCriteria.DEFAULT_SEARCH_COUNT_FLAG))) {
            throw new IllegalArgumentException("loadRelated 目标不支持分页；仅允许默认分页值");
        }
    }

    private Object read(Object bean, String property) {
        try {
            PropertyDescriptor descriptor = descriptor(bean.getClass(), property);
            Method method = descriptor.getReadMethod();
            if (method == null) throw new IllegalArgumentException("属性不可读: " + property);
            if (!method.canAccess(bean)) method.setAccessible(true);
            return method.invoke(bean);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("读取关联属性失败: " + property, exception);
        }
    }

    private void write(Object bean, String property, Object value) {
        try {
            PropertyDescriptor descriptor = descriptor(bean.getClass(), property);
            Method method = descriptor.getWriteMethod();
            if (method == null) throw new IllegalArgumentException("属性不可写: " + property);
            if (!method.canAccess(bean)) method.setAccessible(true);
            method.invoke(bean, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("写入关联属性失败: " + property, exception);
        }
    }

    private PropertyDescriptor descriptor(Class<?> type, String property) {
        PropertyDescriptor descriptor = PROPERTIES.get(type).get(property);
        if (descriptor == null) throw new IllegalArgumentException("实体不存在关联属性: " + type.getName() + "." + property);
        return descriptor;
    }
}
