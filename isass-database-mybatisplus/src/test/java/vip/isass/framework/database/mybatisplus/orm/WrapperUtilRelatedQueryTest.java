// SPDX-License-Identifier: LGPL-3.0-only
package vip.isass.framework.database.mybatisplus.orm;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import vip.isass.framework.common.criteria.JoinCondition;
import vip.isass.framework.common.criteria.WhereCondition;
import vip.isass.framework.common.criteria.impl.type.Condition;
import vip.isass.framework.database.mybatisplus.orm.MybatisPlusExistsTest.Probe;
import vip.isass.framework.database.mybatisplus.orm.MybatisPlusExistsTest.ProbeCriteria;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 动态字段、嵌套参数及 FROM/WHERE 语义的编译合同。
 */
class WrapperUtilRelatedQueryTest {
    private static MpjWrapper<?> queryWrapper(ProbeCriteria criteria) {
        return assertInstanceOf(MpjWrapper.class, WrapperUtil.getQueryWrapper(criteria));
    }

    @BeforeAll
    static void initializeMetadata() {
        MybatisPlusExistsTest.initializeMetadata();
    }

    @Test
    void correlatedSubqueriesUseDistinctAliasesAndPreserveParameterValues() {
        ProbeCriteria child = new ProbeCriteria().equals("label", "child");
        child.notExists(new ProbeCriteria().equals("label", "grandchild"), Probe::getId, Probe::getId);
        ProbeCriteria query = new ProbeCriteria().equals("label", "root").exists(child, Probe::getId, Probe::getId);
        MpjWrapper<?> wrapper = queryWrapper(query);
        String sql = wrapper.querySql();
        assertTrue(sql.contains("EXISTS"), sql);
        assertTrue(sql.contains("NOT EXISTS"), sql);
        assertTrue(sql.contains("t.FILE_ID = tst.FILE_ID"), sql);
        assertTrue(sql.contains("tst.FILE_ID = tstst.FILE_ID"), sql);
        assertEquals(3, wrapper.getParamNameValuePairs().size(), sql);
        assertTrue(wrapper.getParamNameValuePairs().values().containsAll(List.of("root", "child", "grandchild")));
        assertEquals(2, child.getWhereConditions().size());
    }

    @Test
    void groupedSubqueryAndScalarConditionsShareTheCurrentAlias() {
        ProbeCriteria query = new ProbeCriteria().exists(new ProbeCriteria().setWhereConditions(List.of(
                new WhereCondition().setCondition(Condition.Logical.OR).setChildren(List.of(
                        new WhereCondition("label", Condition.Compare.EQUAL, "a"),
                        new WhereCondition("label", Condition.Compare.EQUAL, "b"))))));
        MpjWrapper<?> wrapper = queryWrapper(query);
        String sql = wrapper.querySql();
        assertTrue(sql.contains("tst.FILE_LABEL ="), sql);
        assertFalse(sql.contains(" OR t.FILE_LABEL"), sql);
        assertEquals(2, wrapper.getParamNameValuePairs().size(), wrapper.getFrom() + " / " + wrapper.getSqlSegment()
                + " / " + wrapper.getParamNameValuePairs());
    }

    @Test
    void inRequiresExactlyOneColumnAndDoesNotAppendAnIdentityColumn() {
        ProbeCriteria query = new ProbeCriteria().in("label",
                new ProbeCriteria().setReturnField("label").equals("id", 3L));
        String sql = queryWrapper(query).querySql().replaceAll("\\s+", " ");
        assertTrue(sql.contains("t.FILE_LABEL IN ( SELECT tst.FILE_LABEL FROM"), sql);
        assertThrows(IllegalArgumentException.class, () -> queryWrapper(
                new ProbeCriteria().in("label", new ProbeCriteria())));
        assertThrows(IllegalArgumentException.class, () -> queryWrapper(
                new ProbeCriteria().notIn("label", new ProbeCriteria().setReturnFields(List.of("id", "label")))));
    }

    @Test
    void fromPredicateStaysInsideTheDerivedInput() {
        ProbeCriteria input = new ProbeCriteria().equals("label", "input");
        ProbeCriteria query = new ProbeCriteria().setFromCriteria(input).equals("id", 9L);
        MpjWrapper<?> wrapper = queryWrapper(query);
        String sql = wrapper.querySql().replaceAll("\\s+", " ");
        assertTrue(sql.contains("FROM ( SELECT"), sql);
        assertTrue(sql.indexOf("tst1.FILE_LABEL") < sql.indexOf(") t"), sql);
        assertTrue(sql.contains(") t"), sql);
        assertTrue(sql.contains("t.FILE_ID ="), sql);
        assertEquals(2, wrapper.getParamNameValuePairs().size());
        assertTrue(input.getReturnFields() == null || input.getReturnFields().isEmpty());
    }

    @Test
    void defaultReturnFieldsAreStoredOnCriteriaWithoutSensitiveProperties() {
        ProbeCriteria criteria = new ProbeCriteria();
        MpjWrapper<?> wrapper = queryWrapper(criteria);

        assertTrue(criteria.getReturnFields().containsAll(List.of("id", "label")));
        assertFalse(criteria.getReturnFields().contains("createUserId"));
        assertFalse(wrapper.querySql().contains("CREATE_USER_ID"));

        ProbeCriteria explicit = new ProbeCriteria().setReturnField("createUserId");
        assertTrue(queryWrapper(explicit).querySql().contains("CREATE_USER_ID"));
    }

    @Test
    void joinAddsSourceIdentityToCallingCriteriaWithoutChangingTargetCriteria() {
        ProbeCriteria target = new ProbeCriteria().setReturnField("label");
        ProbeCriteria root = new ProbeCriteria().setReturnField("label")
                .leftJoin(new JoinCondition().setTargetCriteria(target)
                        .setSourceProperty("id").setCondition(Condition.Compare.EQUAL).setTargetProperty("id")
                        .setResultProperty("related"));

        String firstSql = queryWrapper(root).querySql();
        String secondSql = queryWrapper(root).querySql();

        assertTrue(root.getReturnFields().contains("id"));
        assertEquals(List.of("label"), target.getReturnFields());
        assertFalse(firstSql.contains("JOIN ( SELECT"), firstSql);
        assertFalse(secondSql.contains("JOIN ( SELECT"), secondSql);
        assertEquals(firstSql, secondSql);
        JoinCondition relation = root.getJoinConditions().get(0);
        assertEquals("id", relation.getSourceProperty());
        assertEquals("id", relation.getTargetProperty());

        ProbeCriteria sensitiveJoin = new ProbeCriteria().setReturnField("label")
                .leftJoin(new JoinCondition().setTargetCriteria(new ProbeCriteria().setReturnField("label"))
                        .setSourceProperty("createUserId").setCondition(Condition.Compare.EQUAL).setTargetProperty("id")
                        .setResultProperty("related"));
        queryWrapper(sensitiveJoin);
        assertFalse(sensitiveJoin.getReturnFields().contains("createUserId"));
    }

    @Test
    void onlyCurrentQueryJoinsPopulateFromClause() {
        ProbeCriteria joined = new ProbeCriteria().leftJoin(new JoinCondition()
                .setTargetCriteria(new ProbeCriteria())
                .setSourceProperty("id").setCondition(Condition.Compare.EQUAL).setTargetProperty("id")
                .setResultProperty("related"));

        assertFalse(queryWrapper(joined).getFrom().isBlank());
        assertFalse(assertInstanceOf(MpjWrapper.class,
                WrapperUtil.getCountQueryWrapper(joined)).getFrom().isBlank());
        assertTrue(queryWrapper(new ProbeCriteria().exists(joined, Probe::getId, Probe::getId)).getFrom().isBlank());
        assertTrue(queryWrapper(new ProbeCriteria().crossJoin(new JoinCondition()
                .setTargetCriteria(new ProbeCriteria()).setResultProperty("related")))
                .getFrom().contains("CROSS JOIN"));
    }

    @Test
    void writeJoinUsesSameJavaPropertiesWithoutMutatingCriteria() {
        JoinCondition join = new JoinCondition().setTargetCriteria(new ProbeCriteria())
                .setSourceProperty("id").setCondition(Condition.Compare.EQUAL).setTargetProperty("id");
        ProbeCriteria criteria = new ProbeCriteria().leftJoin(join)
                .setWhereConditions(List.of(WhereCondition.eq("id", 7L)));

        var update = WrapperUtil.getJoinUpdateWrapper(criteria);
        var delete = WrapperUtil.getJoinDeleteWrapper(criteria);

        assertTrue(update.getFrom().contains("LEFT JOIN"), update.getFrom());
        assertTrue(update.getFrom().contains("FILE_ID"), update.getFrom());
        assertTrue(update.getSqlSegment().contains("t.FILE_ID"), update.getSqlSegment());
        assertTrue(delete.getFrom().contains("LEFT JOIN"), delete.getFrom());
        assertEquals("id", join.getSourceProperty());
        assertEquals("id", join.getTargetProperty());
    }

    @Test
    void writeColumnComparisonsUseResolvedAliasesWithoutRawComparisonFragments() {
        JoinCondition join = new JoinCondition().setTargetCriteria(new ProbeCriteria())
                .setSourceProperty("id").setCondition(Condition.Compare.LESS_THAN).setTargetProperty("id");
        ProbeCriteria child = new ProbeCriteria().setWhereConditions(List.of(new WhereCondition()
                .setSourceProperty("id").setCondition(Condition.Compare.NOT_EQUAL).setTargetProperty("id")));
        ProbeCriteria criteria = new ProbeCriteria().leftJoin(join).exists(child);

        var wrapper = WrapperUtil.getJoinUpdateWrapper(criteria);

        assertTrue(wrapper.getFrom().contains("t.FILE_ID < ("), wrapper.getFrom());
        assertTrue(wrapper.getSqlSegment().contains("NOT ("), wrapper.getSqlSegment());
        assertTrue(wrapper.getSqlSegment().contains("FILE_ID = ("), wrapper.getSqlSegment());
        assertTrue(wrapper.getParamNameValuePairs().isEmpty());
    }

    @Test
    void writeExistsSharesBoundParameters() {
        ProbeCriteria subquery = new ProbeCriteria()
                .setWhereConditions(List.of(WhereCondition.eq("label", "needle")));
        ProbeCriteria criteria = new ProbeCriteria().exists(subquery, Probe::getId, Probe::getId);

        var update = WrapperUtil.getJoinUpdateWrapper(criteria);

        assertTrue(update.getSqlSegment().contains("EXISTS"), update.getSqlSegment());
        assertEquals(1, update.getParamNameValuePairs().size());
    }

    @Test
    void writeMembershipUsesBoundSubqueryAndSelectedColumn() {
        ProbeCriteria target = new ProbeCriteria().setWhereConditions(List.of(WhereCondition.eq("label", "child")));
        ProbeCriteria criteria = new ProbeCriteria().in(Probe::getId, target, Probe::getId);

        var wrapper = WrapperUtil.getJoinDeleteWrapper(criteria);
        String sql = wrapper.getSqlSegment();

        assertTrue(sql.contains(" IN ("), sql);
        assertTrue(sql.contains("FILE_ID"), sql);
        assertEquals(1, wrapper.getParamNameValuePairs().size());
    }

    @Test
    void nestedWriteExistsUsesDistinctSqlScopes() {
        ProbeCriteria grandchild = new ProbeCriteria().setWhereConditions(List.of(WhereCondition.eq("label", "deep")));
        ProbeCriteria child = new ProbeCriteria().exists(grandchild, Probe::getId, Probe::getId);
        ProbeCriteria criteria = new ProbeCriteria().exists(child, Probe::getId, Probe::getId);

        var wrapper = WrapperUtil.getJoinDeleteWrapper(criteria);
        String sql = wrapper.getSqlSegment();

        assertTrue(sql.contains("st.FILE_ID"), sql);
        assertTrue(sql.contains("stst.FILE_ID"), sql);
        assertEquals(1, wrapper.getParamNameValuePairs().size());
    }

    @Test
    void writeJoinCanFilterItsTargetWithAParameterBoundDerivedTable() {
        ProbeCriteria criteria = new ProbeCriteria().leftJoin(new JoinCondition()
                        .setTargetCriteria(new ProbeCriteria().setWhereConditions(List.of(WhereCondition.eq("label", "child"))))
                        .setSourceProperty("id").setCondition(Condition.Compare.EQUAL).setTargetProperty("id"))
                .setWhereConditions(List.of(WhereCondition.eq("id", 7L)));

        var wrapper = WrapperUtil.getJoinUpdateWrapper(criteria);

        String where = wrapper.getSqlSegment();
        assertTrue(wrapper.getFrom().contains("SELECT"), wrapper.getFrom());
        assertEquals(2, wrapper.getParamNameValuePairs().size(), wrapper.getFrom() + " / " + where
                + " / " + wrapper.getParamNameValuePairs());
    }

    @Test
    void countCompilationDoesNotPopulateDefaultReturnFields() {
        ProbeCriteria criteria = new ProbeCriteria();

        WrapperUtil.getCountQueryWrapper(criteria);

        assertTrue(criteria.getReturnFields().isEmpty());
    }

    @Test
    void existsSelectsOnlyAConstantAndAllowsSiblingCriteriaReuse() {
        ProbeCriteria target = new ProbeCriteria().setReturnField("createUserId").equals("label", "shared");
        ProbeCriteria root = new ProbeCriteria().exists(target).notExists(target);

        MpjWrapper<?> wrapper = queryWrapper(root);
        String sql = wrapper.querySql().replaceAll("\\s+", " ");

        assertTrue(sql.contains("EXISTS ( SELECT 1 FROM"), sql);
        assertFalse(sql.contains("CREATE_USER_ID"), sql);
        assertEquals(List.of("createUserId"), target.getReturnFields());
        assertEquals(2, wrapper.getParamNameValuePairs().size());
        assertEquals(sql, queryWrapper(root).querySql().replaceAll("\\s+", " "));
    }

    @Test
    void fromKeepsOuterColumnsWithoutChangingItsRequestedReturnFields() {
        ProbeCriteria input = new ProbeCriteria().setReturnField("label").equals("label", "input");
        String sql = queryWrapper(new ProbeCriteria().setFromCriteria(input).equals("id", 9L)).querySql();

        assertTrue(sql.contains("tst.FILE_ID"), sql);
        assertEquals(List.of("label"), input.getReturnFields());
    }

    @Test
    void recursiveCriteriaAndConditionCyclesFailBeforeSqlExecution() {
        ProbeCriteria from = new ProbeCriteria();
        from.setFromCriteria(from);
        assertTrue(assertThrows(IllegalArgumentException.class, () -> queryWrapper(from))
                .getMessage().contains("循环引用"));

        ProbeCriteria exists = new ProbeCriteria();
        exists.exists(exists);
        assertTrue(assertThrows(IllegalArgumentException.class, () -> queryWrapper(exists))
                .getMessage().contains("循环引用"));

        ProbeCriteria join = new ProbeCriteria();
        join.leftJoin(new JoinCondition().setTargetCriteria(join).setResultProperty("related")
                .setSourceProperty("id").setTargetProperty("id").setCondition(Condition.Compare.EQUAL));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> queryWrapper(join))
                .getMessage().contains("循环引用"));

        WhereCondition group = new WhereCondition().setChildren(new ArrayList<>());
        group.getChildren().add(group);
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> queryWrapper(new ProbeCriteria().setWhereConditions(List.of(group))))
                .getMessage().contains("循环引用"));

        ProbeCriteria on = new ProbeCriteria().leftJoin(new JoinCondition()
                .setTargetCriteria(new ProbeCriteria()).setResultProperty("related").setChildren(List.of(group)));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> queryWrapper(on))
                .getMessage().contains("循环引用"));
    }
}
