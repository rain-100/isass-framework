package vip.isass.framework.common.exception.code;

import java.lang.invoke.MethodHandles;

/**
 * isass-framework 核心模块标识。
 * 每个模块必须定义自己的 ModuleInfo，通过 MODULE_CODE 区分异常码归属。
 * 异常码格式：MODULE_CODE * 10000 + local_code。
 *
 * @author Rain
 */
public interface ModuleInfo {

    Integer MODULE_CODE = Math.abs(MethodHandles.lookup().lookupClass().getName().hashCode()) % 100000;

    Integer STATUS_CODE_PREFIX = ModuleInfo.MODULE_CODE * 10000;

}
