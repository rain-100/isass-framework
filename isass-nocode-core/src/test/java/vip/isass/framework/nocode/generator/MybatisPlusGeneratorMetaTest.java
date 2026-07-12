package vip.isass.framework.nocode.generator;

import org.junit.jupiter.api.Test;

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
}
