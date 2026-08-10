// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.log.slf4j;

public class TestLogLevelManager implements LogLevelManager {

    static String offLoggerName;

    static String restoreLoggerName;

    static void reset() {
        offLoggerName = null;
        restoreLoggerName = null;
    }

    @Override
    public void loggerOff(String loggerName) {
        offLoggerName = loggerName;
    }

    @Override
    public void loggerRestore(String loggerName) {
        restoreLoggerName = loggerName;
    }
}
