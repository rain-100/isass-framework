// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.generator.mybatisplus;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MybatisPlusGeneratorMetaTest {

    @Test
    void defaultsOverwriteOnlyEntityAndCriteria() {
        MybatisPlusGeneratorMeta meta = new MybatisPlusGeneratorMeta();

        assertTrue(meta.isEntityFileOverride());
        assertTrue(meta.isCriteriaFileOverride());
        assertFalse(meta.isMapperFileOverride());
        assertFalse(meta.isMapperXmlFileOverride());
        assertFalse(meta.isRepositoryFileOverride());
        assertFalse(meta.isServiceInterfaceFileOverride());
        assertFalse(meta.isLocalServiceFileOverride());
        assertFalse(meta.isControllerFileOverride());
    }

    @Test
    void usesContextAsTheDddBoundedContext() {
        MybatisPlusGeneratorMeta meta = new MybatisPlusGeneratorMeta()
                .setContext("attachment");

        assertEquals("attachment", meta.getContext());
    }

    @Test
    void derivesServiceRootPackageFromMultiSegmentServiceContext() {
        MybatisPlusGeneratorMeta meta = new MybatisPlusGeneratorMeta()
                .setPackageName("com.acme")
                .setContext("order.processing.catalog");

        assertEquals("com.acme.order.processing", MybatisPlusGenerator.serviceRootPackageName(meta));
    }
}
