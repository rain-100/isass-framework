package vip.isass.framework.nocode;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * service → 表前缀 静态注册工具。
 * 各模块 DB 配置类在初始化时调用 {@link #register} 注册自己模块的表前缀。
 * 单体模式下多个微服务合并启动，各自注册不冲突。
 *
 * <pre>{@code
 * // AttachmentDbAutoConfiguration
 * @PostConstruct
 * void registerTablePrefix() {
 *     TablePrefixUtil.register(ServiceInfo.SERVICE_FULL_NAME, ServiceInfo.TABLE_PREFIX));
 * }
 * }</pre>
 */
public final class TablePrefixUtil {

    private static final Map<String, String> PREFIX_MAP = new ConcurrentHashMap<>();

    private TablePrefixUtil() {
    }

    public static void register(String service, String prefix) {
        PREFIX_MAP.put(service, prefix);
    }

    public static String get(String service) {
        return PREFIX_MAP.getOrDefault(service, "");
    }
}
