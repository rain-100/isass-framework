package vip.isass.framework.common.log.slf4j;

import cn.hutool.core.lang.Assert;
import lombok.extern.slf4j.Slf4j;

/**
 * 日志工具
 *
 * @author rain
 */
@Slf4j
public class LogUtil {

    private static volatile LogLevelManager logLevelManager = new LogLevelManager.Noop();

    public static void setLogLevelManager(LogLevelManager logLevelManager) {
        LogUtil.logLevelManager = logLevelManager == null ? new LogLevelManager.Noop() : logLevelManager;
    }

    /**
     * 关闭日志
     */
    public static void loggerOff(String loggerName) {
        Assert.notBlank(loggerName, "loggerName 必填");
        logLevelManager.loggerOff(loggerName);
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
        logLevelManager.loggerRestore(loggerName);
    }

    /**
     * 恢复日志级别
     */
    public static void loggerRestore(Class<?> clazz) {
        Assert.notNull(clazz, "clazz 必填");
        loggerRestore(clazz.getName());
    }

}
