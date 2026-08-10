// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.database.mybatisplus.util;

import vip.isass.framework.common.support.ISystemClock;

import java.util.Date;

/**
 * @author Rain
 */
public class SystemClockImpl implements ISystemClock {

    public long now() {
        return com.baomidou.mybatisplus.core.toolkit.SystemClock.now();
    }

    public Date nowDate() {
        return new Date(now());
    }

}
