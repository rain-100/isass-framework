package vip.isass.framework.adapter.springboot.log;

import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggerConfiguration;
import org.springframework.boot.logging.LoggingSystem;
import vip.isass.framework.common.log.slf4j.LogLevelManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SpringBootLogLevelManager implements LogLevelManager {

    private final LoggingSystem loggingSystem;

    private final Map<String, LoggerConfiguration> levelMap = new ConcurrentHashMap<>();

    public SpringBootLogLevelManager(LoggingSystem loggingSystem) {
        this.loggingSystem = loggingSystem;
    }

    @Override
    public void loggerOff(String loggerName) {
        LoggerConfiguration loggerConfiguration = loggingSystem.getLoggerConfiguration(loggerName);
        if (loggerConfiguration != null) {
            levelMap.put(loggerName, loggerConfiguration);
        }
        loggingSystem.setLogLevel(loggerName, LogLevel.OFF);
    }

    @Override
    public void loggerRestore(String loggerName) {
        LoggerConfiguration loggerConfiguration = levelMap.remove(loggerName);
        if (loggerConfiguration == null) {
            return;
        }
        loggingSystem.setLogLevel(loggerName, loggerConfiguration.getConfiguredLevel());
    }
}
