// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.orm;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.reflect.GenericTypeUtils;
import vip.isass.framework.nocode.criteria.ICriteria;
import vip.isass.framework.nocode.criteria.WhereCondition;
import vip.isass.framework.nocode.criteria.type.IOrderByCriteria;
import vip.isass.framework.nocode.criteria.type.IPageCriteria;
import vip.isass.framework.nocode.criteria.type.ISelectColumnCriteria;
import vip.isass.framework.nocode.criteria.type.IWhereConditionCriteria;
import vip.isass.framework.nocode.entity.IEntity;

import java.util.Collection;
import java.util.List;

/**
 * @author Rain
 */
public class WrapperUtil {

    /**
     * 返回 queryWrapper
     *
     * @param <C>      criteria
     * @param <E>      entity
     * @param criteria criteria
     * @return query wrapper
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <
        E extends IEntity<E>,
        C extends ICriteria<E, C>> QueryWrapper<E> getQueryWrapper(ICriteria<E, C> criteria) {
        Class<E> entityClass = currentEntityClass(criteria);
        QueryWrapper<E> wrapper = new QueryWrapper<>();
        wrapper.setEntityClass(entityClass);

        if (criteria instanceof ISelectColumnCriteria) {
            processSelectColumnsCriteria(wrapper, (ISelectColumnCriteria) criteria);
        }

        if (criteria instanceof IWhereConditionCriteria) {
            processWhereConditionCriteria(wrapper, (IWhereConditionCriteria) criteria);
        }

        if (criteria instanceof IPageCriteria) {
            processPageCriteria(wrapper, (IPageCriteria) criteria);
        }

        if (criteria instanceof IOrderByCriteria) {
            processOrderByCriteria(wrapper, (IOrderByCriteria) criteria);
        }

        return wrapper;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <E extends IEntity<E>, C extends ICriteria<E, C>> UpdateWrapper<E> getUpdateWrapper(ICriteria<E, C> criteria) {
        Class<E> entityClass = currentEntityClass(criteria);
        UpdateWrapper<E> wrapper = new UpdateWrapper<>();
        wrapper.setEntityClass(entityClass);

        if (criteria instanceof IWhereConditionCriteria) {
            processWhereConditionCriteria(wrapper, (IWhereConditionCriteria) criteria);
        }

        return wrapper;
    }

    private static <
        E extends IEntity<E>,
        C extends ISelectColumnCriteria<E, C>>
    void processSelectColumnsCriteria(QueryWrapper<E> wrapper, ISelectColumnCriteria<E, C> selectColumnCriteria) {
        Collection<String> selectColumns = selectColumnCriteria.getSelectColumns();
        if (CollUtil.isNotEmpty(selectColumns)) {
            wrapper.select(selectColumns.stream()
                    .map(column -> EntityPropertyColumnResolver.resolve(wrapper.getEntityClass(), column))
                    .toArray(String[]::new));
        }
    }

    private static <
        E extends IEntity<E>,
        C extends IWhereConditionCriteria<E, C>>
    void processWhereConditionCriteria(AbstractWrapper<E, String, ?> wrapper, IWhereConditionCriteria<E, C> whereConditionCriteria) {
        List<WhereCondition> whereConditions = effectiveWhereConditions(whereConditionCriteria.getWhereConditions());
        if (CollUtil.isNotEmpty(whereConditions)) {
            whereConditions.forEach(wc -> MybatisPlusWhereCondition.apply(wc, wrapper));
        }
    }

    private static List<WhereCondition> effectiveWhereConditions(List<WhereCondition> whereConditions) {
        List<WhereCondition> effectiveConditions = new java.util.ArrayList<>();
        boolean pendingOr = false;
        for (WhereCondition whereCondition : whereConditions) {
            if (whereCondition.getCondition() == vip.isass.framework.nocode.criteria.impl.type.Condition.OR) {
                pendingOr = !effectiveConditions.isEmpty();
                continue;
            }
            if (!hasConditionValue(whereCondition)) {
                // OR 只能作用于紧随其后的有效条件，不能跨过被忽略的空条件。
                pendingOr = false;
                continue;
            }
            if (pendingOr) {
                effectiveConditions.add(new WhereCondition(null, vip.isass.framework.nocode.criteria.impl.type.Condition.OR, null));
            }
            effectiveConditions.add(whereCondition);
            pendingOr = false;
        }
        return effectiveConditions;
    }

    private static boolean hasConditionValue(WhereCondition whereCondition) {
        switch (whereCondition.getCondition()) {
            case IS_NULL:
            case IS_NOT_NULL:
            case IS_EMPTY:
            case IS_NOT_EMPTY:
                return true;
            default:
                Object value = whereCondition.getValue();
                if (value == null) {
                    return false;
                }
                if (value instanceof CharSequence) {
                    return StrUtil.isNotBlank((CharSequence) value);
                }
                if (value instanceof Collection) {
                    return CollUtil.isNotEmpty((Collection<?>) value);
                }
                if (value instanceof java.util.Map) {
                    return !((java.util.Map<?, ?>) value).isEmpty();
                }
                return !value.getClass().isArray() || java.lang.reflect.Array.getLength(value) > 0;
        }
    }

    private static <
        E extends IEntity<E>,
        C extends IPageCriteria<E, C>>
    void processPageCriteria(AbstractWrapper<E, String, ?> wrapper, IPageCriteria<E, C> pageCriteria) {
        // do nothing
    }

    private static <
        E extends IEntity<E>,
        C extends IOrderByCriteria<E, C>>
    void processOrderByCriteria(QueryWrapper<E> wrapper,
                                IOrderByCriteria<E, C> criteria) {
        if (StrUtil.isBlank(criteria.getOrderBy())) {
            return;
        }

        String[] orderByColumns = criteria.getOrderBy().split(StrUtil.COMMA);
        for (String orderByColumn : orderByColumns) {
            if (StrUtil.isBlank(orderByColumn)) {
                continue;
            }

            orderByColumn = orderByColumn.trim().replaceAll(" +", StrUtil.SPACE);
            String[] orderByColumnArr = orderByColumn.split(StrUtil.SPACE);
            if (ArrayUtil.isEmpty(orderByColumnArr)) {
                continue;
            }

            switch (orderByColumnArr.length) {
                case 1:
                    wrapper.orderByAsc(EntityPropertyColumnResolver.resolve(wrapper.getEntityClass(), orderByColumnArr[0]));
                    break;
                case 2:
                    if (IOrderByCriteria.ASC.trim().equalsIgnoreCase(orderByColumnArr[1])) {
                        wrapper.orderByAsc(EntityPropertyColumnResolver.resolve(wrapper.getEntityClass(), orderByColumnArr[0]));
                    } else if (IOrderByCriteria.DESC.trim().equalsIgnoreCase(orderByColumnArr[1])) {
                        wrapper.orderByDesc(EntityPropertyColumnResolver.resolve(wrapper.getEntityClass(), orderByColumnArr[0]));
                    } else {
                        throw new IllegalArgumentException("orderBy 参数错误：" + orderByColumn);
                    }
                    break;
                default:
                    throw new IllegalArgumentException("orderBy 参数错误：" + orderByColumn);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <E extends IEntity<E>, C extends ICriteria<E, C>> Class<E> currentEntityClass(ICriteria<E, C> criteria) {
        Class<?>[] typeArguments = GenericTypeUtils.resolveTypeArguments(criteria.getClass(), ICriteria.class);
        if (typeArguments == null || typeArguments.length == 0 || !IEntity.class.isAssignableFrom(typeArguments[0])) {
            throw new IllegalArgumentException("无法从 Criteria 泛型解析实体类型：" + criteria.getClass().getName());
        }
        return (Class<E>) typeArguments[0];
    }

}
