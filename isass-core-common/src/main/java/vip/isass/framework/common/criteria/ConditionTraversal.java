// SPDX-License-Identifier: LGPL-3.0-only
package vip.isass.framework.common.criteria;

import vip.isass.framework.common.criteria.impl.type.Condition;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * 消费条件列表时解析连接符；不在构造阶段或执行前额外扫描整棵树。
 */
public final class ConditionTraversal {
    private ConditionTraversal() {
    }

    public static void consume(List<WhereCondition> conditions, Condition defaultConnector,
                               BiConsumer<Condition, WhereCondition> consumer) {
        Condition connector = defaultConnector == null ? Condition.Logical.AND : defaultConnector;
        if (connector != Condition.Logical.AND && connector != Condition.Logical.OR) {
            throw new IllegalArgumentException("分组默认连接符只能是 AND/OR");
        }
        boolean hasOperand = false;
        Condition pending = null;
        for (WhereCondition node : conditions) {
            Objects.requireNonNull(node, "条件列表不能包含 null");
            if (isSeparator(node)) {
                if (!hasOperand || pending != null) {
                    throw new IllegalArgumentException("AND/OR 不能位于开头或连续出现");
                }
                pending = node.getCondition();
                continue;
            }
            consumer.accept(hasOperand ? pending == null ? connector : pending : null, node);
            hasOperand = true;
            pending = null;
        }
        if (pending != null) throw new IllegalArgumentException("AND/OR 不能位于末尾");
    }

    public static boolean isSeparator(BaseCondition<?> node) {
        return (node.getCondition() == Condition.Logical.AND || node.getCondition() == Condition.Logical.OR)
                && node.getSourceProperty() == null && node.getTargetProperty() == null
                && node.getValue() == null && node.getChildren() == null && node.getTargetCriteria() == null;
    }
}
