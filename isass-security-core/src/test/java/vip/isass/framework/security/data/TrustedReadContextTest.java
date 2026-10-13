// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.security.data;

import org.junit.jupiter.api.Test;
import vip.isass.framework.common.criteria.WhereCondition;
import vip.isass.framework.common.criteria.impl.type.FullTypeCriteria;
import vip.isass.framework.common.entity.IEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrustedReadContextTest {
    @Test
    void bindsOnlyTheExactCriteriaAndDoesNotPersistConstraintsInItsParameters() {
        Criteria criteria = new Criteria();
        WhereCondition restriction = WhereCondition.eq("tenantId", 7L);
        try (var ignored = TrustedReadContext.open(criteria, List.of(restriction))) {
            assertEquals(List.of(restriction), TrustedReadContext.conditions(criteria));
            assertTrue(TrustedReadContext.conditions(new Criteria()).isEmpty());
            assertTrue(criteria.getWhereConditions().isEmpty());
            assertThrows(UnsupportedOperationException.class,
                    () -> TrustedReadContext.conditions(criteria).clear());
        }
        assertTrue(TrustedReadContext.conditions(criteria).isEmpty());
    }

    @Test
    void nestedRestrictionsOnlyAccumulateAndRestoreAfterFailure() {
        Criteria criteria = new Criteria();
        WhereCondition outer = WhereCondition.eq("tenantId", 7L);
        WhereCondition inner = WhereCondition.eq("enabledFlag", true);
        try (var ignored = TrustedReadContext.open(criteria, List.of(outer))) {
            assertThrows(IllegalStateException.class, () -> {
                try (var nested = TrustedReadContext.open(criteria, List.of(inner))) {
                    assertEquals(List.of(inner, outer), TrustedReadContext.conditions(criteria));
                    DataReadContext.metadata(() -> {
                        assertEquals(List.of(inner, outer), TrustedReadContext.conditions(criteria));
                        return null;
                    });
                    throw new IllegalStateException("failed read");
                }
            });
            assertEquals(List.of(outer), TrustedReadContext.conditions(criteria));
        }
        assertTrue(TrustedReadContext.conditions(criteria).isEmpty());
    }

    @Test
    void childQueriesCannotInheritAnotherCriteriaObjectsRestrictions() {
        Criteria outer = new Criteria();
        Criteria inner = new Criteria();
        WhereCondition restriction = WhereCondition.eq("tenantId", 7L);
        try (var ignored = TrustedReadContext.open(outer, List.of(restriction));
             var child = TrustedReadContext.open(inner, List.of())) {
            assertEquals(List.of(restriction), TrustedReadContext.conditions(outer));
            assertTrue(TrustedReadContext.conditions(inner).isEmpty());
        }
        assertTrue(TrustedReadContext.conditions(outer).isEmpty());
    }

    private record Entity() implements IEntity<Entity> {
        @Override
        public Entity randomEntity() {
            return this;
        }
    }

    private static final class Criteria extends FullTypeCriteria<Entity, Criteria> {
    }
}
