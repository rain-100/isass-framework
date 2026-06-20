package vip.isass.framework.common.log.slf4j;

public interface LogLevelManager {

    void loggerOff(String loggerName);

    void loggerRestore(String loggerName);

    class Noop implements LogLevelManager {

        @Override
        public void loggerOff(String loggerName) {
        }

        @Override
        public void loggerRestore(String loggerName) {
        }
    }
}
