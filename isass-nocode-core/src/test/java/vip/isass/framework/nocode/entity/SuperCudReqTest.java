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
        assertTrue(request.addIfAbsentItems().isEmpty());
        assertTrue(request.updateEntities().isEmpty());
        assertTrue(request.updateByCriteriaItems().isEmpty());
        assertTrue(request.deleteIds().isEmpty());
        assertTrue(request.deleteCriteria().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> request.addEntities().add("third"));
    }

}
