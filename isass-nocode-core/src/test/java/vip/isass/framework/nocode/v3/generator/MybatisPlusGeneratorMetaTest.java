package vip.isass.framework.nocode.v3.generator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MybatisPlusGeneratorMetaTest {

    @Test
    void defaultsOnlyKeepEntityAndCriteriaFromBeingOverwritten() {
        MybatisPlusGeneratorMeta meta = new MybatisPlusGeneratorMeta();

        assertFalse(meta.isEntityFileOverride());
        assertFalse(meta.isCriteriaFileOverride());
        assertTrue(meta.isMapperFileOverride());
        assertTrue(meta.isMapperXmlFileOverride());
        assertTrue(meta.isRepositoryFileOverride());
        assertTrue(meta.isServiceInterfaceFileOverride());
        assertTrue(meta.isLocalServiceFileOverride());
        assertTrue(meta.isControllerFileOverride());
    }
}
