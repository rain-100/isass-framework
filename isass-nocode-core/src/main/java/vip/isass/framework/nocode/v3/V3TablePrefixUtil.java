package vip.isass.framework.nocode.v3;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * serviceName → 表前缀 静态注册工具。
 * 各模块 DB 配置类在初始化时调用 {@link #register} 注册自己模块的表前缀。
 * 单体模式下多个微服务合并启动，各自注册不冲突。
 *
 * <pre>{@code
 * // AttachmentDbAutoConfiguration
 * @PostConstruct
 * void registerTablePrefix() {
 *     V3TablePrefixUtil.register("attachment-service", "att_");
 * }
 * }</pre>
 */
public final class V3TablePrefixUtil {

    private static final Map<String, String> PREFIX_MAP = new ConcurrentHashMap<>();

    private V3TablePrefixUtil() {
    }

    public static void register(String serviceName, String prefix) {
        PREFIX_MAP.put(serviceName, prefix);
    }

    public static String get(String serviceName) {
        return PREFIX_MAP.getOrDefault(serviceName, "");
    }
}
