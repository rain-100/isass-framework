// SPDX-License-Identifier: LGPL-3.0-only
package vip.isass.framework.database.mybatisplus.orm;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import vip.isass.framework.common.criteria.JoinCondition;
import vip.isass.framework.common.criteria.WhereCondition;
import vip.isass.framework.common.criteria.impl.type.Condition;
import vip.isass.framework.common.security.data.DataReadContext;
import vip.isass.framework.common.security.data.DataReadPolicy;
import vip.isass.framework.database.mybatisplus.orm.MybatisPlusExistsTest.Probe;
import vip.isass.framework.database.mybatisplus.orm.MybatisPlusExistsTest.ProbeCriteria;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class DataReadPolicySqlTest {
    @BeforeAll
    static void metadata() {
        MybatisPlusExistsTest.initializeMetadata();
    }

    @Test
    void clientOrCannotEscapeTheAddedAndRestriction() {
        ProbeCriteria criteria = new ProbeCriteria().setWhereConditions(List.of(
                WhereCondition.eq("label", "a"), new WhereCondition().setCondition(Condition.Logical.OR),
                WhereCondition.eq("label", "b")));
        try (var ignored = DataReadContext.open(policy())) {
            MpjWrapper<?> wrapper = (MpjWrapper<?>) WrapperUtil.getQueryWrapper(criteria);
            String sql = wrapper.getSqlSegment();
            assertTrue(sql.contains("OR") && sql.contains(") AND ("), sql);
            assertTrue(wrapper.getParamNameValuePairs().containsValue(7L));
        }
        assertNull(DataReadContext.current());
    }

    @Test
    void scopeIsAppliedToFromJoinExistsAndInEvenWhenTheTargetHasNoLogicDelete() {
        ProbeCriteria criteria = new ProbeCriteria()
                .setFromCriteria(new ProbeCriteria())
                .leftJoin(new JoinCondition()
                        .setTargetCriteria(new ProbeCriteria())
                        .setSourceProperty("id")
                        .setCondition(Condition.Compare.EQUAL)
                        .setTargetProperty("id")
                        .setResultProperty("related"))
                .exists(new ProbeCriteria().equals("label", "child"))
                .in("id", new ProbeCriteria().setReturnField("id"));
        try (var ignored = DataReadContext.open(policy())) {
            MpjWrapper<?> wrapper = (MpjWrapper<?>) WrapperUtil.getQueryWrapper(criteria);
            String sql = wrapper.querySql().replaceAll("\\s+", " ");
            assertTrue(sql.contains("JOIN (SELECT") || sql.contains("JOIN ( SELECT"), sql);
            assertEquals(5, wrapper.getParamNameValuePairs().values().stream().filter(value -> value.equals(7L)).count(), sql);
            assertTrue(sql.contains("EXISTS") && sql.contains(" IN "), sql);
            MpjWrapper<?> count = (MpjWrapper<?>) WrapperUtil.getCountQueryWrapper(criteria);
            count.querySql();
            assertEquals(5, count.getParamNameValuePairs().values().stream().filter(value -> value.equals(7L)).count());
        }
    }

    private static DataReadPolicy policy() {
        return type -> List.of(WhereCondition.eq("id", 7L));
    }

    @Test
    void protectedFieldsCannotBecomeFilterOrderJoinOrScalarSubqueryInputs() {
        DataReadPolicy protectedField = new DataReadPolicy() {
            @Override
            public List<WhereCondition> conditions(Class<?> type) {
                return List.of(WhereCondition.eq("label", "server-only-scope"));
            }

            @Override
            public void requireQueryable(Class<?> type, String property) {
                if ("label".equals(property)) {
                    throw new IllegalArgumentException("protected field");
                }
            }
        };
        try (var ignored = DataReadContext.open(protectedField)) {
            assertDoesNotThrow(() -> WrapperUtil.getQueryWrapper(new ProbeCriteria()));
            assertThrows(IllegalArgumentException.class,
                    () -> WrapperUtil.getCountQueryWrapper(new ProbeCriteria().equals("label", "secret")));
            assertThrows(IllegalArgumentException.class,
                    () -> WrapperUtil.getQueryWrapper(new ProbeCriteria().setOrderBy("label ASC")));
            assertThrows(IllegalArgumentException.class,
                    () -> WrapperUtil.getQueryWrapper(new ProbeCriteria().in("id", new ProbeCriteria().setReturnField("label"))));
            assertThrows(IllegalArgumentException.class, () -> WrapperUtil.getQueryWrapper(new ProbeCriteria()
                    .leftJoin(new JoinCondition().setTargetCriteria(new ProbeCriteria()).setSourceProperty("label")
                            .setCondition(Condition.Compare.EQUAL).setTargetProperty("label").setResultProperty("related"))));
        }
    }
}
