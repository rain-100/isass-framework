// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.entity;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SuperCudReqTest {

    @Test
    void normalizesNullGroupsAndDefensivelyCopiesLists() {
        List<String> entities = new ArrayList<>(List.of("first"));

        SuperCudReq<String, String> request = new SuperCudReq<>(entities, null, null, null, null, null);
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
    void builderResolvesLambdaPropertiesAndNormalizesFieldNames() {
        TestCriteria criteria = new TestCriteria();

        SuperCudReq<TestEntity, TestCriteria> request = SuperCudReq.<TestEntity, TestCriteria>builder()
                .addEntity(new TestEntity())
                .addByFields(TestEntity::getCode)
                .updateEntity(new TestEntity())
                .updateByCriteria(criteria, TestEntity::getCode)
                .build();

        assertEquals(List.of("code"), request.addByFields());
        assertEquals(List.of("code"), criteria.resolveMatchFields());
    }

    static final class TestEntity implements IEntity<TestEntity> {
        public String getCode() {
            return "code";
        }

        @Override
        public TestEntity randomEntity() {
            return this;
        }
    }

    static final class TestCriteria
            extends vip.isass.framework.nocode.criteria.impl.type.FullTypeCriteria<TestEntity, TestCriteria> {
    }

}
