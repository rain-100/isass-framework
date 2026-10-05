// SPDX-License-Identifier: LGPL-3.0-only
package vip.isass.framework.common.criteria;

import vip.isass.framework.common.criteria.impl.type.Condition;
import vip.isass.framework.common.entity.EntityAssociation;

import java.util.List;

/**
 * 在执行投影准备阶段解析 JOIN 的实体关联属性，不依赖数据库或 ORM。
 */
public final class JoinResultPropertyResolver {
    private JoinResultPropertyResolver() {
    }

    public static EntityAssociation resolve(Class<?> sourceType, JoinCondition condition) {
        Class<?> targetType = CriteriaEntityTypes.entityClass(condition.getTargetCriteria());
        List<EntityAssociation> candidates = CriteriaMetadata.associations(sourceType).stream()
                .filter(relation -> relation.targetType() == targetType)
                .filter(relation -> condition.getResultProperty() != null
                        ? relation.property().equals(condition.getResultProperty())
                        : condition.getChildren() == null && condition.getCondition() == Condition.Compare.EQUAL
                        && relation.localField().equals(condition.getSourceProperty())
                        && relation.targetField().equals(condition.getTargetProperty()))
                .toList();
        if (candidates.size() != 1) {
            throw new IllegalArgumentException("JOIN 结果字段无法唯一确定或与关联元数据不符，请设置 resultProperty: "
                    + sourceType.getSimpleName() + " -> " + targetType.getSimpleName());
        }
        return candidates.getFirst();
    }
}
