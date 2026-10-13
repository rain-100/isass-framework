// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.security.data;

import org.junit.jupiter.api.Test;
import vip.isass.framework.common.criteria.WhereCondition;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TenantReadContextTest {
    @Test
    void roleMetadataAndUnrestrictedRolePoliciesCannotRemoveTenantIsolation() {
        TenantReadPolicy tenant = type -> List.of(WhereCondition.eq("tenantId", 7L));
        try (var ignored = TenantReadContext.open(tenant);
             var role = DataReadContext.open(type -> List.of())) {
            DataReadContext.metadata(() -> {
                assertNull(DataReadContext.current());
                assertSame(tenant, TenantReadContext.current());
                return null;
            });
            assertSame(tenant, TenantReadContext.current());
        }
        assertNull(TenantReadContext.current());
        assertNull(DataReadContext.current());
    }

    @Test
    void nestedScopesRestoreTheOuterBoundaryAfterFailure() {
        TenantReadPolicy outer = type -> List.of(WhereCondition.eq("tenantId", 7L));
        TenantReadPolicy inner = type -> List.of(WhereCondition.eq("tenantId", 8L));
        try (var ignored = TenantReadContext.open(outer)) {
            assertThrows(IllegalStateException.class, () -> {
                try (var nested = TenantReadContext.open(inner)) {
                    assertSame(inner, TenantReadContext.current());
                    throw new IllegalStateException("query failed");
                }
            });
            assertSame(outer, TenantReadContext.current());
            assertThrows(NullPointerException.class, () -> TenantReadContext.open(null));
            assertSame(outer, TenantReadContext.current());
        }
        assertNull(TenantReadContext.current());
    }
}
