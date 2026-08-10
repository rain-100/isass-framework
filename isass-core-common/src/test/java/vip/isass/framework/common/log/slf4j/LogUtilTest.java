// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.log.slf4j;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LogUtilTest {

    @Test
    void initializesLogLevelManagerFromServiceLoader() {
        try {
            TestLogLevelManager.reset();
            LogUtil.setLogLevelManagerFromServiceLoader();

            LogUtil.loggerOff("test.logger");
            LogUtil.loggerRestore("test.logger");

            assertThat(TestLogLevelManager.offLoggerName).isEqualTo("test.logger");
            assertThat(TestLogLevelManager.restoreLoggerName).isEqualTo("test.logger");
        } finally {
            LogUtil.setLogLevelManager(null);
            TestLogLevelManager.reset();
        }
    }
}
