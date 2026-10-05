// SPDX-License-Identifier: LGPL-3.0-only
package vip.isass.framework.common.criteria;

import org.junit.jupiter.api.Test;
import vip.isass.framework.common.criteria.impl.type.Condition;
import vip.isass.framework.common.support.JsonUtil;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConditionTest {

    @Test
    void groupedEnumsKeepExistingJsonNames() {
        WhereCondition original = new WhereCondition("name", Condition.Compare.EQUAL, "admin");
        String json = JsonUtil.writeValue(original);
        assertTrue(json.contains("\"condition\":\"EQUAL\""), json);
        assertEquals(Condition.Compare.EQUAL,
                JsonUtil.readValue(json, WhereCondition.class).getCondition());
        assertEquals(Condition.Compare.EQUAL, Condition.fromName("EQUAL"));
        assertThrows(IllegalArgumentException.class, () -> Condition.fromName("UNKNOWN"));
    }

    @Test
    void everyGroupedOperatorRoundTripsThroughCriteriaJson() {
        Stream.of(Condition.Logical.values(), Condition.Compare.values(), Condition.Membership.values(),
                        Condition.NullCheck.values(), Condition.Text.values(), Condition.Array.values(),
                        Condition.Json.values(), Condition.Existence.values())
                .flatMap(Arrays::stream)
                .forEach(condition -> {
                    WhereCondition original = new WhereCondition().setCondition(condition);
                    WhereCondition restored = JsonUtil.readValue(JsonUtil.writeValue(original), WhereCondition.class);
                    assertEquals(condition, restored.getCondition());
                    assertEquals(condition, Condition.fromName(condition.name()));
                });
    }
}
