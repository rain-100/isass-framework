// SPDX-License-Identifier: LGPL-3.0-only
package vip.isass.framework.common.criteria;

import org.junit.jupiter.api.Test;
import vip.isass.framework.common.criteria.impl.type.FullTypeCriteria;
import vip.isass.framework.common.criteria.impl.type.Condition;
import vip.isass.framework.common.entity.EntityAssociation;
import vip.isass.framework.common.entity.IEntity;
import vip.isass.framework.common.security.data.DataReadPolicy;
import vip.isass.framework.common.security.data.DataReadProjection;
import vip.isass.framework.common.security.data.HiddenDataFields;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DataReadProjectionTest {
    private final DataReadPolicy policy = new DataReadPolicy() {
        @Override
        public List<WhereCondition> conditions(Class<?> entityType) {
            return List.of();
        }

        @Override
        public Set<String> requiredProperties(Class<?> entityType) {
            return Set.of("ownerId");
        }
    };

    @Test
    void joinAndLoadRelatedPreserveRequestedFieldsAndHideOnlyInternalKeys() {
        for (boolean join : List.of(true, false)) {
            NodeCriteria related = new NodeCriteria().addReturnFields("label");
            NodeCriteria root = new NodeCriteria().addReturnFields("ownerId", "label");
            if (join) {
                root.leftJoin(new JoinCondition().setTargetCriteria(related).setResultProperty("child")
                        .setSourceProperty("ownerId").setCondition(Condition.Compare.EQUAL).setTargetProperty("ownerId"));
            } else {
                root.loadRelated("child", related);
            }
            var projection = DataReadProjection.capture(Node.class, root, policy);
            Node value = new Node();
            value.child = new Node();
            projection.hideInternalProperties(List.of(value));
            assertFalse(HiddenDataFields.contains(value, "ownerId"));
            assertTrue(HiddenDataFields.contains(value.child, "ownerId"));
            assertFalse(HiddenDataFields.contains(value.child, "label"));
            assertEquals(1L, value.child.ownerId);
        }
    }

    @Test
    void defaultProjectionDoesNotHideOrdinaryKeysAndTreeRecursionDoesNotLoop() {
        Node value = new Node();
        value.child = value;
        DataReadProjection.capture(Node.class, new NodeCriteria(), policy).hideInternalProperties(value);
        assertFalse(HiddenDataFields.contains(value, "ownerId"));
        DataReadProjection.capture(Node.class, new NodeCriteria().addReturnFields("label"), policy)
                .hideInternalProperties(value);
        assertTrue(HiddenDataFields.contains(value, "ownerId"));
    }

    @Test
    void dottedLoadPathsCaptureTheTerminalProjectionWithoutRestrictingTheImplicitParent() {
        Node value = new Node();
        value.child = new Node();
        value.child.child = new Node();
        var criteria = new NodeCriteria().loadRelated("child.child", new NodeCriteria().addReturnFields("label"));
        DataReadProjection.capture(Node.class, criteria, policy).hideInternalProperties(value);
        assertFalse(HiddenDataFields.contains(value.child, "ownerId"));
        assertTrue(HiddenDataFields.contains(value.child.child, "ownerId"));
    }

    static final class Node implements IEntity<Node> {
        private Long ownerId = 1L;
        private String label;
        private Node child;

        @Override
        public List<EntityAssociation> associations() {
            return List.of(EntityAssociation.one("child", Node.class, "ownerId", "ownerId"));
        }

        @Override
        public Node randomEntity() {
            return this;
        }
    }

    static final class NodeCriteria extends FullTypeCriteria<Node, NodeCriteria> {
    }
}
