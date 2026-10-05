// SPDX-License-Identifier: LGPL-3.0-only
package vip.isass.framework.database.mybatisplus.orm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class BaseColumnFactoryTest {

    @Test
    void createsAnIndependentColumnRootForEachTableOccurrence() {
        var first = BaseColumnFactory.create(MybatisPlusExistsTest.Probe.class);
        var second = BaseColumnFactory.create(MybatisPlusExistsTest.Probe.class);

        assertSame(MybatisPlusExistsTest.Probe.class, first.getColumnClass());
        assertSame(MybatisPlusExistsTest.Probe.class, second.getColumnClass());
        assertNotSame(first, second);
    }
}
