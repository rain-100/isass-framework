package vip.isass.framework.common.exception.code;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ModuleInfoTest {

    @Test
    void moduleCodeIs5Digits() {
        assertThat(ModuleInfo.MODULE_CODE).isBetween(0, 99999);
    }

    @Test
    void statusCodePrefixFollowsPattern() {
        assertThat(ModuleInfo.STATUS_CODE_PREFIX)
                .isEqualTo(ModuleInfo.MODULE_CODE * 10000);
    }
}
