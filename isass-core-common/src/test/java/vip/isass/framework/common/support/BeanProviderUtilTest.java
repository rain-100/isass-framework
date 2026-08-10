// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BeanProviderUtilTest {

    @Test
    void initializesBeanProviderFromServiceLoader() {
        try {
            BeanProviderUtil.setBeanProviderFromServiceLoader();

            assertThat(BeanProviderUtil.isInitialized()).isTrue();
            assertThat(BeanProviderUtil.getContext()).isEqualTo("test-context");
        } finally {
            BeanProviderUtil.setBeanProvider(null);
        }
    }
}
