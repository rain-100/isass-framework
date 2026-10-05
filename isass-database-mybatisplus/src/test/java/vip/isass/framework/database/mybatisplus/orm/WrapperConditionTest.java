// SPDX-License-Identifier: LGPL-3.0-only
package vip.isass.framework.database.mybatisplus.orm;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.github.yulichang.query.MPJQueryWrapper;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import vip.isass.framework.common.criteria.EmptyCriteria;
import vip.isass.framework.common.criteria.WhereCondition;
import vip.isass.framework.common.criteria.impl.type.Condition;
import vip.isass.framework.common.support.BeanProviderUtil;
import vip.isass.framework.common.support.JsonUtil;
import vip.isass.framework.database.mybatisplus.orm.MybatisPlusExistsTest.Probe;
import vip.isass.framework.database.mybatisplus.orm.MybatisPlusExistsTest.ProbeCriteria;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class WrapperConditionTest {
    @BeforeAll
    static void initializeMetadata() { MybatisPlusExistsTest.initializeMetadata(); }

    @Test
    void consumesNestedBooleanExpressionsWithoutDroppingFilters() {
        WhereCondition nested = new WhereCondition().setCondition(Condition.Logical.OR).setChildren(List.of(
                new WhereCondition("label", Condition.Compare.EQUAL, "a"),
                new WhereCondition().setCondition(Condition.Logical.AND),
                new WhereCondition("id", Condition.Compare.GREATER_THAN, 10L),
                new WhereCondition("label", Condition.Compare.EQUAL, "b")));
        ProbeCriteria query = new ProbeCriteria().setWhereConditions(List.of(
                new WhereCondition("id", Condition.Compare.EQUAL, 1L),
                new WhereCondition().setCondition(Condition.Logical.OR), nested));
        var wrapper = assertInstanceOf(MpjWrapper.class, WrapperUtil.getQueryWrapper(query));
        String sql = wrapper.getSqlSegment();
        String operators = sql.replace("(", "").replace(")", "");
        assertTrue(operators.contains(" OR t.FILE_LABEL ="), sql);
        assertTrue(operators.contains(" AND t.FILE_ID >"), sql);
        assertEquals(4, wrapper.getParamNameValuePairs().size());
        assertEquals(3, query.getWhereConditions().size());
    }

    @Test
    void invalidConditionsFailBeforeAnyMapperCanExecute() {
        WhereCondition leaf = new WhereCondition("id", Condition.Compare.EQUAL, 1L);
        WhereCondition or = new WhereCondition().setCondition(Condition.Logical.OR);
        for (List<WhereCondition> invalid : List.of(List.of(or, leaf), List.of(leaf, or),
                List.of(leaf, or, or, leaf), List.of(new WhereCondition("id", Condition.Compare.EQUAL, null)),
                List.of(new WhereCondition().setChildren(List.of())), List.of(WhereCondition.and()),
                List.of(WhereCondition.or()))) {
            ProbeCriteria criteria = new ProbeCriteria().setWhereConditions(invalid);
            assertThrows(IllegalArgumentException.class, () -> WrapperUtil.getQueryWrapper(criteria));
            assertThrows(IllegalArgumentException.class, () -> WrapperUtil.getUpdateWrapper(criteria));
        }
        ProbeCriteria nullChild = new ProbeCriteria().setWhereConditions(List.of(WhereCondition.not(null)));
        assertThrows(NullPointerException.class, () -> WrapperUtil.getQueryWrapper(nullChild));
    }

    @Test
    void convenienceGroupsCompileLikeTheExistingConditionTree() {
        var wrapper = assertInstanceOf(MpjWrapper.class, WrapperUtil.getQueryWrapper(new ProbeCriteria().setWhereConditions(List.of(
                WhereCondition.or(WhereCondition.eq("id", 1L),
                        WhereCondition.and(WhereCondition.eq("label", "a"),
                                WhereCondition.not(WhereCondition.eq("id", 2L))))))));
        String sql = wrapper.getSqlSegment();
        assertTrue(sql.contains(" OR ("), sql);
        assertTrue(sql.contains(" AND NOT ("), sql);
        assertEquals(3, wrapper.getParamNameValuePairs().size());
    }

    @Test
    void scalarConditionsUseNativeMpAndMpjWrappers() {
        WhereCondition equal = new WhereCondition("label", Condition.Compare.EQUAL, "needle");
        QueryWrapper<Probe> mp = new QueryWrapper<>(Probe.class);
        WrapperUtil.applyMpCondition(equal, mp);
        assertTrue(mp.getSqlSegment().contains("FILE_LABEL ="));
        assertEquals(1, mp.getParamNameValuePairs().size());
        assertEquals("label", equal.getSourceProperty());
        assertFalse(JsonUtil.writeValue(equal).contains("sourceColumn"));
        assertFalse(JsonUtil.writeValue(equal).contains("targetColumn"));

        UpdateWrapper<Probe> mpUpdate = new UpdateWrapper<>();
        mpUpdate.setEntityClass(Probe.class);
        WrapperUtil.applyMpCondition(
                new WhereCondition("id", Condition.Membership.NOT_IN, List.of(1L, 2L)), mpUpdate);
        WrapperUtil.applyMpCondition(
                new WhereCondition("label", Condition.NullCheck.IS_NULL, null), mpUpdate);
        assertTrue(mpUpdate.getSqlSegment().contains("FILE_ID NOT IN"), mpUpdate.getSqlSegment());
        assertTrue(mpUpdate.getSqlSegment().contains("FILE_LABEL IS NULL"), mpUpdate.getSqlSegment());
        assertEquals(2, mpUpdate.getParamNameValuePairs().size());

        MPJQueryWrapper<Probe> mpjString = new MPJQueryWrapper<Probe>(Probe.class).setAlias("source");
        WrapperUtil.applyMpCondition(equal, mpjString);
        assertTrue(mpjString.getSqlSegment().contains("source.FILE_LABEL ="), mpjString.getSqlSegment());
        assertEquals("label", equal.getSourceProperty());

        MpjWrapper<Probe> apt = new MpjWrapper<>(Probe.class);
        WrapperUtil.applyAptCondition(apt, equal, apt.getBaseColumn(), null);
        WrapperUtil.applyAptCondition(apt,
                new WhereCondition("id", Condition.Membership.IN, List.of(1L, 2L)), apt.getBaseColumn(), null);
        WrapperUtil.applyAptCondition(apt,
                new WhereCondition("label", Condition.Array.CONTAINS_ANY, List.of("a", "b")), apt.getBaseColumn(), null);
        assertTrue(apt.getSqlSegment().contains("t.FILE_LABEL ="), apt.getSqlSegment());
        assertTrue(apt.getSqlSegment().contains("t.FILE_ID IN"), apt.getSqlSegment());
        assertTrue(apt.getSqlSegment().contains("t.FILE_LABEL &&"), apt.getSqlSegment());
        assertEquals(4, apt.getParamNameValuePairs().size());
        assertEquals("label", equal.getSourceProperty());

        MpjWrapper<Probe> arrayValues = new MpjWrapper<>(Probe.class);
        WrapperUtil.applyAptCondition(arrayValues,
                new WhereCondition("id", Condition.Membership.IN, new Long[]{1L, 2L}),
                arrayValues.getBaseColumn(), null);
        assertTrue(arrayValues.getSqlSegment().contains("t.FILE_ID IN"));
        assertEquals(2, arrayValues.getParamNameValuePairs().size());

        MPJLambdaWrapper<Probe> lambda = new MPJLambdaWrapper<>(Probe.class);
        WrapperUtil.applyJoinCondition(equal, lambda);
        assertTrue(lambda.getSqlSegment().contains("t.FILE_LABEL ="), lambda.getSqlSegment());
        assertEquals("label", equal.getSourceProperty());

        MPJLambdaWrapper<Probe> joined = new MPJLambdaWrapper<>(Probe.class);
        joined.leftJoin(Probe.class, on -> {
            WrapperUtil.applyJoinCondition(equal, on);
            return on;
        });
        assertTrue(joined.getFrom().contains("t1.FILE_LABEL ="), joined.getFrom());
        assertEquals("label", equal.getSourceProperty());
        equal.setSourceProperty("id");
        assertEquals("id", equal.getSourceProperty());
    }

    @Test
    void plainMpComparesTwoColumnsWithoutTableAliases() {
        ProbeCriteria criteria = new ProbeCriteria().setWhereConditions(List.of(new WhereCondition()
                .setSourceProperty("id").setCondition(Condition.Compare.LESS_THAN)
                .setTargetProperty("createUserId")));

        UpdateWrapper<Probe> wrapper = WrapperUtil.getUpdateWrapper(criteria);

        assertTrue(wrapper.getSqlSegment().contains("FILE_ID < (CREATE_USER_ID)"), wrapper.getSqlSegment());
        assertFalse(wrapper.getSqlSegment().contains("t."), wrapper.getSqlSegment());
        assertTrue(wrapper.getParamNameValuePairs().isEmpty());
    }

    @Test
    void jsonConditionsUseMpjAliasAndParameterBinding() throws Exception {
        DynamicRoutingDataSource dataSource = mock(DynamicRoutingDataSource.class);
        DatabaseIdProvider databaseIdProvider = mock(DatabaseIdProvider.class);
        when(databaseIdProvider.getDatabaseId(dataSource)).thenReturn("mysql");
        try (MockedStatic<BeanProviderUtil> beans = mockStatic(BeanProviderUtil.class)) {
            beans.when(() -> BeanProviderUtil.getBean(DynamicRoutingDataSource.class)).thenReturn(dataSource);
            beans.when(() -> BeanProviderUtil.getBean(DatabaseIdProvider.class)).thenReturn(databaseIdProvider);
            MpjWrapper<Probe> wrapper = new MpjWrapper<>(Probe.class);

            WrapperUtil.applyAptCondition(wrapper,
                    new WhereCondition("label.key", Condition.Json.JSON_OBJECT_PATH_EQUAL, "a"),
                    wrapper.getBaseColumn(), null);
            WrapperUtil.applyAptCondition(wrapper,
                    new WhereCondition("label.key", Condition.Json.JSON_OBJECT_PATH_LIKE, "partial"),
                    wrapper.getBaseColumn(), null);
            WrapperUtil.applyAptCondition(wrapper,
                    new WhereCondition("label", Condition.Json.JSON_ARRAY_CONTAINS, "b"),
                    wrapper.getBaseColumn(), null);
            WrapperUtil.applyAptCondition(wrapper,
                    new WhereCondition("label", Condition.Json.JSON_ARRAY_CONTAINS_ANY, List.of("c", "d")),
                    wrapper.getBaseColumn(), null);

            assertTrue(wrapper.getSqlSegment().contains("t.FILE_LABEL->'$.key'"), wrapper.getSqlSegment());
            assertTrue(wrapper.getSqlSegment().contains("JSON_CONTAINS(t.FILE_LABEL,"), wrapper.getSqlSegment());
            assertTrue(wrapper.getSqlSegment().contains("AND (JSON_CONTAINS(t.FILE_LABEL,"), wrapper.getSqlSegment());
            assertTrue(wrapper.getSqlSegment().contains(" OR JSON_CONTAINS(t.FILE_LABEL,"), wrapper.getSqlSegment());
            assertFalse(wrapper.getSqlSegment().contains("CONCAT"), wrapper.getSqlSegment());
            assertEquals(5, wrapper.getParamNameValuePairs().size());

            QueryWrapper<Probe> mp = new QueryWrapper<>(Probe.class);
            WrapperUtil.applyMpCondition(
                    new WhereCondition("label.key", Condition.Json.JSON_OBJECT_PATH_EQUAL, "a"), mp);
            WrapperUtil.applyMpCondition(
                    new WhereCondition("label", Condition.Json.JSON_ARRAY_CONTAINS_ALL, List.of("c", "d")), mp);
            assertTrue(mp.getSqlSegment().contains("FILE_LABEL->'$.key'"), mp.getSqlSegment());
            assertTrue(mp.getSqlSegment().contains("AND (JSON_CONTAINS(FILE_LABEL,"), mp.getSqlSegment());
            assertTrue(mp.getSqlSegment().contains("AND JSON_CONTAINS(FILE_LABEL,"), mp.getSqlSegment());
            assertEquals(3, mp.getParamNameValuePairs().size());

            MPJQueryWrapper<Probe> stringQuery = new MPJQueryWrapper<>(Probe.class);
            WrapperUtil.applyMpCondition(
                    new WhereCondition("label", Condition.Json.JSON_ARRAY_CONTAINS, "b"), stringQuery);
            assertTrue(stringQuery.getSqlSegment().contains("JSON_CONTAINS(t.FILE_LABEL,"),
                    stringQuery.getSqlSegment());
            assertEquals(1, stringQuery.getParamNameValuePairs().size());

            MPJLambdaWrapper<Probe> lambda = new MPJLambdaWrapper<>(Probe.class);
            WrapperUtil.applyJoinCondition(
                    new WhereCondition("label", Condition.Json.JSON_ARRAY_CONTAINS_ANY, List.of("c", "d")), lambda);
            assertTrue(lambda.getSqlSegment().contains(" OR JSON_CONTAINS(t.FILE_LABEL,"), lambda.getSqlSegment());
            assertEquals(2, lambda.getParamNameValuePairs().size());
        }
    }

    @Test
    void damengJsonFallbackUsesNativeLikeAndGroupsMultipleValues() throws Exception {
        DynamicRoutingDataSource dataSource = mock(DynamicRoutingDataSource.class);
        DatabaseIdProvider databaseIdProvider = mock(DatabaseIdProvider.class);
        when(databaseIdProvider.getDatabaseId(dataSource)).thenReturn("dm");
        try (MockedStatic<BeanProviderUtil> beans = mockStatic(BeanProviderUtil.class)) {
            beans.when(() -> BeanProviderUtil.getBean(DynamicRoutingDataSource.class)).thenReturn(dataSource);
            beans.when(() -> BeanProviderUtil.getBean(DatabaseIdProvider.class)).thenReturn(databaseIdProvider);

            QueryWrapper<Probe> plain = new QueryWrapper<>(Probe.class);
            WrapperUtil.applyMpCondition(
                    new WhereCondition("label.key", Condition.Json.JSON_OBJECT_PATH_EQUAL, "a"), plain);
            assertTrue(plain.getSqlSegment().contains("FILE_LABEL LIKE"), plain.getSqlSegment());
            assertEquals("%a%", plain.getParamNameValuePairs().values().iterator().next());

            MpjWrapper<Probe> apt = new MpjWrapper<>(Probe.class);
            WrapperUtil.applyAptCondition(apt,
                    new WhereCondition("label", Condition.Json.JSON_ARRAY_CONTAINS_ANY, List.of("a", "b")),
                    apt.getBaseColumn(), null);
            assertTrue(apt.getSqlSegment().contains("t.FILE_LABEL LIKE"), apt.getSqlSegment());
            assertTrue(apt.getSqlSegment().contains(" OR "), apt.getSqlSegment());
            assertFalse(apt.getSqlSegment().contains("CONCAT"), apt.getSqlSegment());
            assertEquals(2, apt.getParamNameValuePairs().size());
        }
    }

    @Test
    void unsupportedRelationalWritesCannotFallBackToUnfilteredUpdates() {
        ProbeCriteria criteria = new ProbeCriteria().leftJoin(Probe.class, Probe::getId, Probe::getId);
        assertThrows(UnsupportedOperationException.class, () -> WrapperUtil.getUpdateWrapper(criteria));
        ProbeCriteria subquery = new ProbeCriteria().exists(new ProbeCriteria().equals("id", 1L));
        assertThrows(UnsupportedOperationException.class, () -> WrapperUtil.getUpdateWrapper(subquery));
        assertEquals(Probe.class, assertInstanceOf(MpjWrapper.class,
                WrapperUtil.getQueryWrapper(EmptyCriteria.of(Probe.class))).getEntityClass());
    }
}
