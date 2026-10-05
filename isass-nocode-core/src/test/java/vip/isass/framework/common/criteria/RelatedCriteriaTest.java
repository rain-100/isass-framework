// SPDX-License-Identifier: LGPL-3.0-only
package vip.isass.framework.common.criteria;

import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.Test;
import vip.isass.framework.common.support.JsonUtil;
import vip.isass.framework.common.criteria.impl.type.Condition;
import vip.isass.framework.common.criteria.impl.type.FullTypeCriteria;
import vip.isass.framework.common.entity.IIdEntity;
import vip.isass.framework.nocode.query.CriteriaQueryParamConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RelatedCriteriaTest {
    @Test
    void restoresScalarConvenienceSettersWithoutVarargsArrayCoercion() {
        CriteriaEntityTypes.register(ItemCriteria.class);
        ItemCriteria query = JsonUtil.readValue(
                "{\"id\":7,\"fromCriteria\":{\"entityType\":\"item\",\"id\":9}}", ItemCriteria.class);
        assertEquals(7L, query.getEquals("id", Long.class));
        assertEquals(9L, ((ItemCriteria) query.getFromCriteria()).getEquals("id", Long.class));
    }
    @Test
    void nestedCriteriaRoundTripsThroughOrdinaryJsonAndQueryJsonExactlyOnce() {
        CriteriaEntityTypes.register(ItemCriteria.class);
        ItemCriteria child = new ItemCriteria().equals("name", "a&b,中文");
        ItemCriteria source = new ItemCriteria().leftJoin(Item.class, Item::getId, Item::getId)
                .exists(child).loadRelated("items", child).setFromCriteria(new ItemCriteria().equals("id", 99L));
        String json = JsonUtil.writeValue(source);
        ItemCriteria restored = JsonUtil.readValue(json, ItemCriteria.class);
        assertEquals(99L, ((ItemCriteria) restored.getFromCriteria()).getEquals("id", Long.class));
        assertSame(EmptyCriteria.of(Item.class), restored.getJoinConditions().getFirst().getTargetCriteria());
        assertEquals("a&b,中文", ((ItemCriteria) restored.getLoadRelated().getFirst().getCriteria()).getEquals("name"));
        assertEquals(1, restored.getWhereConditions().size());

        CriteriaQueryParamConverter converter = new CriteriaQueryParamConverter();
        Map<String, String> query = converter.toQueryParams(source, ItemCriteria.class);
        assertTrue(query.get("joinConditions").startsWith("["));
        assertTrue(query.get("whereConditions").contains("\"targetCriteria\":{"));
        ItemCriteria fromQuery = (ItemCriteria) converter.fromQueryParams(query, ItemCriteria.class);
        assertEquals(1, fromQuery.getWhereConditions().size());
        assertEquals("a&b,中文", ((ItemCriteria) fromQuery.getWhereConditions().getFirst().getTargetCriteria()).getEquals("name"));
        assertThrows(IllegalArgumentException.class, () -> converter.fromQueryParams(Map.of("association.query", "items"), ItemCriteria.class));
        assertThrows(RuntimeException.class, () -> JsonUtil.readValue(
                "{\"entityType\":\"java.lang.Runtime\"}", ICriteria.class));
        assertThrows(RuntimeException.class, () -> JsonUtil.readValue(
                "{\"entityType\":\"other\"}", ItemCriteria.class));
    }

    @Test
    void oldConditionFieldsAreRejectedInsteadOfSilentlyDroppingTheFilter() {
        assertThrows(RuntimeException.class, () -> JsonUtil.readValue(
                "{\"propertyName\":\"id\",\"condition\":\"EQUAL\",\"value\":1}", WhereCondition.class));
        WhereCondition restored = JsonUtil.readValue(
                "{\"sourceProperty\":\"id\",\"condition\":\"EQUAL\",\"value\":2099073034202943490}", WhereCondition.class);
        assertEquals(2099073034202943490L, restored.getValue());
        assertEquals("id", restored.getSourceProperty());
    }

    @Test
    void copiesNestedRequestStateWithoutCallingConvenienceSetters() {
        ItemCriteria source = new ItemCriteria();
        source.setName("original");
        ItemCriteria target = new ItemCriteria().equals("id", new ArrayList<>(List.of(1L)));
        source.leftJoin(target, Item::getId, Item::getId);
        source.loadRelated("items", target);
        source.setFromCriteria(new ItemCriteria().equals("name", "input"));
        source.getWhereConditions().add(new WhereCondition().setChildren(new ArrayList<>(List.of(
                new WhereCondition("name", Condition.Compare.EQUAL, "nested")))));

        ItemCriteria copy = source.copy();
        assertEquals(2, copy.getWhereConditions().size());
        copy.getWhereConditions().getLast().getChildren().getFirst().setValue("changed");
        ItemCriteria copiedTarget = (ItemCriteria) copy.getJoinConditions().getFirst().getTargetCriteria();
        copiedTarget.equals("name", "new");
        ((ItemCriteria) copy.getLoadRelated().getFirst().getCriteria()).equals("id", 2L);
        ((ItemCriteria) copy.getFromCriteria()).equals("id", 3L);

        assertEquals("nested", source.getWhereConditions().getLast().getChildren().getFirst().getValue());
        assertEquals(1, target.getWhereConditions().size());
        assertEquals(1, ((ItemCriteria) source.getJoinConditions().getFirst().getTargetCriteria()).getWhereConditions().size());
        assertEquals(1, ((ItemCriteria) source.getLoadRelated().getFirst().getCriteria()).getWhereConditions().size());
        assertEquals(1, ((ItemCriteria) source.getFromCriteria()).getWhereConditions().size());
    }

    @Test
    void reusesEmptySourcesWithoutLeakingClassNamesOrMutableState() {
        EmptyCriteria<Item> empty = EmptyCriteria.of(Item.class);
        ItemCriteria query = new ItemCriteria().leftJoin(Item.class, Item::getId, Item::getId);
        assertSame(empty, query.getJoinConditions().getFirst().getTargetCriteria());
        assertSame(empty, query.copy().getJoinConditions().getFirst().getTargetCriteria());
        assertEquals(Map.of("entityType", "item"), JsonUtil.convertToMap(empty));
        assertEquals("item", query.getEntityType());
        assertNotSame(empty, EmptyCriteria.of(Other.class));
    }

    @Test
    void convenienceMethodsMutateSharedInputsAndExplicitCopyIsolatesThem() {
        ItemCriteria child = new ItemCriteria().equals("name", "child");
        List<WhereCondition> originalConditions = child.getWhereConditions();
        ItemCriteria parent = new ItemCriteria().exists(child, Item::getId, Item::getId)
                .in(Item::getId, child, Item::getId);
        assertSame(child, parent.getWhereConditions().getFirst().getTargetCriteria());
        assertSame(child, parent.getWhereConditions().getLast().getTargetCriteria());
        assertSame(originalConditions, child.getWhereConditions().getFirst().getChildren());
        assertEquals("id", child.getWhereConditions().getLast().getTargetProperty());
        assertEquals(List.of("id"), child.getReturnFields());

        ItemCriteria isolated = new ItemCriteria().notExists(child.copy(), Item::getId, Item::getId);
        originalConditions.getFirst().setValue("changed");
        assertTrue(JsonUtil.writeValue(parent).contains("changed"));
        assertTrue(!JsonUtil.writeValue(isolated).contains("changed"));
        assertEquals(2, child.getWhereConditions().size());
    }

    @Test
    void emptySubqueriesMaterializeIndependentlyBeforeConvenienceMutation() {
        CriteriaEntityTypes.register(ItemCriteria.class);
        EmptyCriteria<Item> empty = EmptyCriteria.of(Item.class);
        ItemCriteria parent = new ItemCriteria().exists(empty, Item::getId, Item::getId)
                .in(Item::getId, empty, Item::getId);
        ItemCriteria exists = (ItemCriteria) parent.getWhereConditions().getFirst().getTargetCriteria();
        ItemCriteria in = (ItemCriteria) parent.getWhereConditions().getLast().getTargetCriteria();
        assertNotSame(exists, in);
        assertEquals(1, exists.getWhereConditions().size());
        assertTrue(exists.getReturnFields().isEmpty());
        assertTrue(in.getWhereConditions().isEmpty());
        assertEquals(List.of("id"), in.getReturnFields());
        assertEquals(Map.of("entityType", "item"), JsonUtil.convertToMap(empty));
    }

    @Test
    void registeredRelationsAndListsReflectChangesUntilAnExplicitSnapshot() {
        ItemCriteria target = new ItemCriteria();
        JoinCondition join = new JoinCondition().setTargetCriteria(target)
                .setSourceProperty("id").setCondition(Condition.Compare.EQUAL).setTargetProperty("id");
        RelatedCondition related = new RelatedCondition().setProperty("items").setCriteria(target);
        List<WhereCondition> filters = new ArrayList<>();
        List<String> columns = new ArrayList<>();
        List<JoinCondition> joins = new ArrayList<>();
        List<RelatedCondition> relations = new ArrayList<>();
        ItemCriteria query = new ItemCriteria().setWhereConditions(filters).setReturnFields(columns)
                .setJoinConditions(joins).setLoadRelated(relations).setFromCriteria(target)
                .leftJoin(join).loadRelated(related);
        ItemCriteria snapshot = query.copy();
        filters.add(WhereCondition.eq("name", "late"));
        columns.add("id");
        target.equals("name", "target");
        join.setResultProperty("otherItems");
        related.setProperty("otherItems");
        assertEquals(JoinType.LEFT, join.getJoinType());
        assertSame(join, joins.getFirst());
        assertSame(related, relations.getFirst());
        assertEquals("late", query.getEquals("name"));
        assertEquals(List.of("id"), query.getReturnFields());
        assertTrue(JsonUtil.writeValue(query).contains("\"returnFields\""));
        assertTrue(!JsonUtil.writeValue(query).contains("\"selectColumns\""));
        assertEquals("target", ((ItemCriteria) query.getFromCriteria()).getEquals("name"));
        assertTrue(JsonUtil.writeValue(query).contains("otherItems"));
        assertTrue(snapshot.getWhereConditions().isEmpty());
        assertTrue(snapshot.getReturnFields().isEmpty());
        assertTrue(!JsonUtil.writeValue(snapshot).contains("otherItems"));
    }

    @Test
    void cachedBindingsPreserveInheritedGenericTypesAndDoNotShareRequestState() {
        // 同一泛型基类在两个具体 Criteria 中解析不同类型；并发请求不共享字段值或条件树。
        IntStream.range(0, 64).parallel().forEach(index -> {
            NumericCriteria numeric = JsonUtil.readValue(
                    "{\"payload\":" + index + ",\"probe\":" + index + "}", NumericCriteria.class);
            TextCriteria text = JsonUtil.readValue(
                    "{\"payload\":\"text\",\"probe\":\"value\"}", TextCriteria.class);
            assertEquals((long) index, numeric.getPayload());
            assertEquals((long) index, numeric.getEquals("probe", Long.class));
            assertEquals("text", text.getPayload());
            assertEquals("value", text.getEquals("probe", String.class));
            NumericCriteria copy = numeric.copy();
            copy.getWhereConditions().getFirst().setValue(-1L);
            assertEquals((long) index, numeric.getEquals("probe", Long.class));
        });
    }

    @Test
    void conditionFactoriesKeepJsonShapeAndRetainInputReferences() {
        List<Long> values = new ArrayList<>(List.of(7L));
        WhereCondition leaf = WhereCondition.eq("id", values);
        WhereCondition expression = WhereCondition.or(leaf,
                WhereCondition.and(WhereCondition.eq(Item::getName, "a"),
                        WhereCondition.not(WhereCondition.eq(Item::getId, 9L))));
        String json = JsonUtil.writeValue(expression);
        WhereCondition restored = JsonUtil.readValue(json, WhereCondition.class);
        assertEquals(JsonUtil.readTree(json), JsonUtil.valueToTree(restored));
        values.add(8L);
        assertEquals(List.of(7L, 8L), expression.getChildren().getFirst().getValue());
        leaf.setValue("changed");
        assertEquals("changed", expression.getChildren().getFirst().getValue());
        expression.getChildren().getFirst().setValue("owned");
        assertEquals("owned", leaf.getValue());
        WhereCondition fields = WhereCondition.eq(Item::getId, Other::getId);
        assertEquals("id", fields.getSourceProperty());
        assertEquals("id", fields.getTargetProperty());
        assertNull(fields.getValue());
    }

    @Test
    void rejectsCyclesDuringCopyButAllowsRepeatedSubtrees() {
        ItemCriteria criteria = new ItemCriteria();
        WhereCondition cyclic = new WhereCondition();
        cyclic.setChildren(List.of(cyclic));
        criteria.getWhereConditions().add(cyclic);
        assertThrows(IllegalArgumentException.class, criteria::copy);
        WhereCondition leaf = new WhereCondition("id", Condition.Compare.EQUAL, 1L);
        criteria.setWhereConditions(List.of(leaf, leaf));
        assertEquals(2, criteria.copy().getWhereConditions().size());
    }

    @Test
    void parentConnectorAndExplicitSeparatorAreIndependent() {
        WhereCondition a = new WhereCondition("id", Condition.Compare.EQUAL, 1L);
        WhereCondition b = new WhereCondition("id", Condition.Compare.EQUAL, 2L);
        List<Condition> links = new ArrayList<>();
        ConditionTraversal.consume(List.of(a, new WhereCondition().setCondition(Condition.Logical.AND), b, a),
                Condition.Logical.OR, (link, node) -> links.add(link));
        assertNull(links.getFirst());
        assertEquals(Condition.Logical.AND, links.get(1));
        assertEquals(Condition.Logical.OR, links.get(2));
        WhereCondition or = new WhereCondition().setCondition(Condition.Logical.OR);
        for (List<WhereCondition> invalid : List.of(List.of(or, a), List.of(a, or), List.of(a, or, or, b))) {
            assertThrows(IllegalArgumentException.class, () -> ConditionTraversal.consume(invalid, null, (link, node) -> { }));
        }
    }

    @Getter @Setter
    static class Item implements IIdEntity<Long, Item> {
        private Long id;
        private String name;
        @Override public Item randomEntity() { return this; }
    }
    static class ItemCriteria extends FullTypeCriteria<Item, ItemCriteria> {
        public ItemCriteria setId(Long id) { return equals("id", id); }
        private String name;
        public void setName(String value) { name = value; equals("name", value); }
        public String getName() { return name; }
    }
    static class GenericMetadataCriteria<V, C extends GenericMetadataCriteria<V, C>> extends FullTypeCriteria<Item, C> {
        private V payload;
        public V getPayload() { return payload; }
        public void setProbe(V value) { equals("probe", value); }
    }
    static class NumericCriteria extends GenericMetadataCriteria<Long, NumericCriteria> { }
    static class TextCriteria extends GenericMetadataCriteria<String, TextCriteria> { }
    @Getter @Setter
    static class Other implements IIdEntity<Long, Other> {
        private Long id;
        @Override public Other randomEntity() { return this; }
    }
}
