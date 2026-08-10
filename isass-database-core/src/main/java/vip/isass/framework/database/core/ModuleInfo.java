// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.database.core;

import java.lang.invoke.MethodHandles;

public interface ModuleInfo {

    /**
     * 5位数的模块编号
     */
    Integer MODULE_CODE = Math.abs(MethodHandles.lookup().lookupClass().getName().hashCode()) % 100000;

    Integer STATUS_CODE_PREFIX = ModuleInfo.MODULE_CODE * 10000;

}
