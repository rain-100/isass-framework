// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import vip.isass.framework.common.criteria.impl.type.FullTypeCriteria;
import vip.isass.framework.common.entity.IEntity;
import vip.isass.framework.common.entity.IIdEntity;
import vip.isass.framework.common.support.JsonUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SuperCudReqTest {

    @Test
    void normalizesNullGroupsAndDefensivelyCopiesLists() {
        List<String> entities = new ArrayList<>(List.of("first"));

        SuperCudReq<String, String, Serializable> request = new SuperCudReq<>(entities, null, null, null, null, null);
        entities.add("second");

        assertEquals(List.of("first"), request.addEntities());
        assertTrue(request.addByFields().isEmpty());
        assertTrue(request.updateEntities().isEmpty());
        assertEquals(null, request.updateCriteria());
        assertTrue(request.deleteIds().isEmpty());
        assertTrue(request.deleteCriteria().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> request.addEntities().add("third"));
    }

    @Test
    void buildsCriteriaBasedChangesForEntitiesWithoutPrimaryKeys() {
        KeylessEntity created = new KeylessEntity("created");
        KeylessEntity updated = new KeylessEntity("updated");
        KeylessCriteria updateCriteria = new KeylessCriteria().equals(KeylessEntity::getCode, "before");
        KeylessCriteria deleteCriteria = new KeylessCriteria().equals(KeylessEntity::getCode, "obsolete");

        SuperCudReq<KeylessEntity, KeylessCriteria, Serializable> request =
                SuperCudReq.<KeylessEntity, KeylessCriteria, Serializable>builder()
                        .addEntity(created)
                        .addByFields(KeylessEntity::getCode)
                        .updateEntity(updated)
                        .updateByCriteria(updateCriteria, KeylessEntity::getCode)
                        .deleteByCriteria(deleteCriteria)
                        .build();

        assertEquals(List.of(created), request.addEntities());
        assertEquals(List.of("code"), request.addByFields());
        assertEquals(List.of(updated), request.updateEntities());
        assertEquals(updateCriteria, request.updateCriteria());
        assertEquals(List.of(deleteCriteria), request.deleteCriteria());
        assertTrue(request.deleteIds().isEmpty());
        assertEquals(List.of("code"), updateCriteria.resolveMatchFields());
        assertEquals(List.of(created),
                SuperCudReq.<KeylessEntity, KeylessCriteria, Serializable>add(created).addEntities());
        assertEquals(List.of(updated),
                SuperCudReq.<KeylessEntity, KeylessCriteria, Serializable>update(updated).updateEntities());
        assertEquals(List.of("code"), SuperCudReq
                .<KeylessEntity, KeylessCriteria, Serializable>addIfAbsent(created, KeylessEntity::getCode)
                .addByFields());
        assertEquals(List.of(deleteCriteria), SuperCudReq
                .<KeylessEntity, KeylessCriteria, Serializable>deleteByCriteria(deleteCriteria).deleteCriteria());
    }

    @Test
    void deserializesEntityPayloadsWithoutPrimaryKeys() {
        SuperCudReq<KeylessEntity, KeylessCriteria, Serializable> request = JsonUtil.readValue("""
                {"addEntities":[{"code":"created"}],
                 "updateEntities":[{"code":"updated"}]}
                """, new TypeReference<SuperCudReq<KeylessEntity, KeylessCriteria, Serializable>>() {});

        assertEquals(List.of(new KeylessEntity("created")), request.addEntities());
        assertEquals(List.of(new KeylessEntity("updated")), request.updateEntities());
        assertTrue(request.deleteIds().isEmpty());
    }

    @Test
    void builderResolvesLambdaPropertiesAndNormalizesFieldNames() {
        TestCriteria criteria = new TestCriteria();

        SuperCudReq<TestEntity, TestCriteria, Long> request = SuperCudReq.<TestEntity, TestCriteria, Long>builder()
                .addEntity(new TestEntity())
                .addByFields(TestEntity::getCode)
                .updateEntity(new TestEntity())
                .updateByCriteria(criteria, TestEntity::getCode)
                .deleteId(2087700000000000020L)
                .build();

        assertEquals(List.of("code"), request.addByFields());
        assertEquals(List.of("code"), criteria.resolveMatchFields());
        assertEquals(List.of(2087700000000000020L), request.deleteIds());
    }

    @Test
    void deserializesStringAndNumericIdsAsExactLongs() {
        SuperCudReq<TestEntity, TestCriteria, Long> request = JsonUtil.readValue("""
                {"addEntities":[{"id":"2087700000000000020"}],
                 "deleteIds":["2087700000000000020",9223372036854775807,"-9223372036854775808"]}
                """, new TypeReference<SuperCudReq<TestEntity, TestCriteria, Long>>() {});

        assertEquals(2087700000000000020L, request.addEntities().getFirst().getId());
        assertInstanceOf(Long.class, request.deleteIds().getFirst());
        assertEquals(List.of(2087700000000000020L, Long.MAX_VALUE, Long.MIN_VALUE), request.deleteIds());
    }

    @Test
    void preservesStringPrimaryKeysAndResolvesIntegerPrimaryKeys() {
        SuperCudReq<KeyedEntity<String>, Object, String> strings = JsonUtil.readValue(
                "{\"deleteIds\":[\"000123\",\"user-key\"]}",
                new TypeReference<SuperCudReq<KeyedEntity<String>, Object, String>>() {});
        SuperCudReq<KeyedEntity<Integer>, Object, Integer> integers = JsonUtil.readValue(
                "{\"deleteIds\":[\"123\",2147483647]}",
                new TypeReference<SuperCudReq<KeyedEntity<Integer>, Object, Integer>>() {});

        assertEquals(List.of("000123", "user-key"), strings.deleteIds());
        assertInstanceOf(Integer.class, integers.deleteIds().getFirst());
        assertEquals(List.of(123, Integer.MAX_VALUE), integers.deleteIds());
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"\"", "\"not-an-id\"", "\"100.5\"", "\"1e2\"",
            "\"9223372036854775808\"", "9223372036854775808", "100.5", "1e2", "null"})
    void rejectsInvalidIdsDuringDeserialization(String jsonId) {
        assertThrows(JacksonException.class, () -> JsonUtil.readValue(
                "{\"deleteIds\":[" + jsonId + "]}",
                new TypeReference<SuperCudReq<TestEntity, TestCriteria, Long>>() {}));
    }

    public static final class TestEntity implements IIdEntity<Long, TestEntity> {
        private Long id;

        @Override
        public Long getId() {
            return id;
        }

        @Override
        public void setId(Long id) {
            this.id = id;
        }

        public String getCode() {
            return "code";
        }

        @Override
        public TestEntity randomEntity() {
            return this;
        }
    }

    public static final class TestCriteria extends FullTypeCriteria<TestEntity, TestCriteria> {
    }

    public record KeylessEntity(String code) implements IEntity<KeylessEntity> {

        public String getCode() {
            return code;
        }

        @Override
        public KeylessEntity randomEntity() {
            return this;
        }
    }

    public static final class KeylessCriteria extends FullTypeCriteria<KeylessEntity, KeylessCriteria> {
    }

    public static final class KeyedEntity<PK extends Serializable> implements IIdEntity<PK, KeyedEntity<PK>> {
        private PK id;

        @Override
        public PK getId() {
            return id;
        }

        @Override
        public void setId(PK id) {
            this.id = id;
        }
    }

}
