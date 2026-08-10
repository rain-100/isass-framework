// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.exception.code;

/**
 * 模块异常码解析工具。
 * 异常码格式：MODULE_CODE * 10000 + local_code。
 * 所有模块都必须有 ModuleInfo.MODULE_CODE，不存在"通用模块码"。
 *
 * @author Rain
 */
public final class ModuleCodeResolver {

    private ModuleCodeResolver() {
    }

    /** 模块前缀乘数：每个模块可容纳 0~9999 的本地异常码 */
    public static final int MODULE_PREFIX_MULTIPLIER = 10000;

    /**
     * 从异常码中提取模块编号。
     * 例如 statusCode=100251001 返回 10025。
     */
    public static int resolveModuleCode(int statusCode) {
        return statusCode / MODULE_PREFIX_MULTIPLIER;
    }

    /**
     * 用模块编号和本地码组合成完整异常码。
     */
    public static int compose(int moduleCode, int localCode) {
        return moduleCode * MODULE_PREFIX_MULTIPLIER + localCode;
    }

}
