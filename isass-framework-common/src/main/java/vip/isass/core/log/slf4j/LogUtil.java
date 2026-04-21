package vip.isass.core.log.slf4j;

import cn.hutool.core.lang.Assert;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggerConfiguration;
import org.springframework.boot.logging.LoggingSystem;
import vip.isass.core.support.SpringContextUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 日志工具
 *
 * @author rain
 */
@Slf4j
public class LogUtil {

    public static final Map<String, LoggerConfiguration> LEVEL_MAP = new ConcurrentHashMap<>();

    /**
     * 关闭日志
     */
    public static void loggerOff(String loggerName) {
        Assert.notBlank(loggerName, "loggerName 必填");
        LoggingSystem loggingSystem = SpringContextUtil.getBean(LoggingSystem.class);
        LoggerConfiguration loggerConfiguration = loggingSystem.getLoggerConfiguration(loggerName);
        if (loggerConfiguration != null) {
            LEVEL_MAP.put(loggerName, loggerConfiguration);
        }
        loggingSystem.setLogLevel(loggerName, LogLevel.OFF);
    }

    /**
     * 关闭日志
     */
    public static void loggerOff(Class<?> clazz) {
        Assert.notNull(clazz, "clazz 必填");
        loggerOff(clazz.getName());
    }

    /**
     * 恢复日志级别
     */
    public static void loggerRestore(String loggerName) {
        Assert.notBlank(loggerName, "loggerName 必填");
        LoggerConfiguration loggerConfiguration = LEVEL_MAP.remove(loggerName);
        if (loggerConfiguration == null) {
            return;
        }
        LoggingSystem loggingSystem = SpringContextUtil.getBean(LoggingSystem.class);
        loggingSystem.setLogLevel(loggerName, loggerConfiguration.getConfiguredLevel());
    }

    /**
     * 恢复日志级别
     */
    public static void loggerRestore(Class<?> clazz) {
        Assert.notNull(clazz, "clazz 必填");
        loggerRestore(clazz.getName());
    }

}
