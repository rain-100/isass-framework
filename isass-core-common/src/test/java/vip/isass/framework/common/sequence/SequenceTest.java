// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.sequence;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SequenceTest {

    @Test
    void stableLongIdKeepsPersistentProtocolCompatible() {
        assertEquals(4819283706837468048L, Sequence.stableLongId("tenant-app:1:2"));
        assertEquals(7986022554257708212L,
                Sequence.stableLongId("auth-resource:bsp-service/auth/bootstrap#register"));
        assertEquals(2097458424736379321L, Sequence.stableLongId("permission-resource:1:2"));
    }

    @Test
    void stableLongIdRejectsMissingIdentity() {
        assertThrows(IllegalArgumentException.class, () -> Sequence.stableLongId(null));
        assertThrows(IllegalArgumentException.class, () -> Sequence.stableLongId("  "));
    }
}
