// SPDX-License-Identifier: LGPL-3.0-only
package vip.isass.framework.common.criteria;

import org.junit.jupiter.api.Test;
import vip.isass.framework.common.criteria.impl.type.FullTypeCriteria;
import vip.isass.framework.common.entity.IEntity;
import vip.isass.framework.common.support.JsonUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CriteriaEntityTypesTest {

    @Test
    void readingEntityTypeDoesNotRegisterCriteria() {
        GetterCriteria criteria = new GetterCriteria();
        assertEquals("getterEntity", criteria.getEntityType());
        assertEquals("getterEntity", EmptyCriteria.of(GetterEntity.class).getEntityType());
        JsonUtil.writeValue(criteria);
        assertThrows(IllegalArgumentException.class, () -> CriteriaEntityTypes.requireEntity("getterEntity"));

        CriteriaEntityTypes.register(GetterCriteria.class);
        assertSame(GetterEntity.class, CriteriaEntityTypes.requireEntity("getterEntity"));
        assertEquals(GetterCriteria.class, CriteriaEntityTypes.newCriteria(GetterEntity.class).getClass());
    }

    @Test
    void concreteCriteriaDeserializationRegistersItsType() {
        String json = JsonUtil.writeValue(new DeserializedCriteria());
        JsonUtil.readValue(json, DeserializedCriteria.class);
        assertSame(DeserializedEntity.class, CriteriaEntityTypes.requireEntity("deserializedEntity"));
    }

    static class GetterEntity implements IEntity<GetterEntity> {
        @Override
        public GetterEntity randomEntity() {
            return this;
        }
    }

    static class GetterCriteria extends FullTypeCriteria<GetterEntity, GetterCriteria> { }

    static class DeserializedEntity implements IEntity<DeserializedEntity> {
        @Override
        public DeserializedEntity randomEntity() {
            return this;
        }
    }

    static class DeserializedCriteria extends FullTypeCriteria<DeserializedEntity, DeserializedCriteria> { }
}
