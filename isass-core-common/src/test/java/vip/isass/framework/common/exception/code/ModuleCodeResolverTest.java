package vip.isass.framework.common.exception.code;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ModuleCodeResolverTest {

    @Test
    void resolveModuleCodeFromPrefixedCode() {
        assertThat(ModuleCodeResolver.resolveModuleCode(100251001)).isEqualTo(10025);
    }

    @Test
    void resolveModuleCode() {
        assertThat(ModuleCodeResolver.resolveModuleCode(403)).isEqualTo(0);
    }

    @Test
    void composePrefixedCode() {
        assertThat(ModuleCodeResolver.compose(10025, 1001)).isEqualTo(100251001);
    }

    @Test
    void composeCode() {
        assertThat(ModuleCodeResolver.compose(0, 403)).isEqualTo(403);
    }
}
