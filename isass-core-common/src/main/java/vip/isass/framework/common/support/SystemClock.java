// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.support;

import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.function.Supplier;

/**
 * @author Rain
 */
@Slf4j
public class SystemClock {

    private volatile static ISystemClock iSystemClock;
    private volatile static Supplier<ISystemClock> systemClockProvider = () -> null;

    public static void setSystemClock(ISystemClock systemClock) {
        SystemClock.iSystemClock = systemClock;
        SystemClock.systemClockProvider = () -> systemClock;
    }

    public static void setSystemClockProvider(Supplier<ISystemClock> systemClockProvider) {
        SystemClock.iSystemClock = null;
        SystemClock.systemClockProvider = systemClockProvider == null ? () -> null : systemClockProvider;
    }

    public static long now() {
        loadSystemClockImpl();
        return iSystemClock == null ? System.currentTimeMillis() : iSystemClock.now();
    }

    public static Date nowDate() {
        loadSystemClockImpl();
        return iSystemClock == null ? new Date() : iSystemClock.nowDate();
    }

    private static void loadSystemClockImpl() {
        if (iSystemClock == null) {
            synchronized (SystemClock.class) {
                if (iSystemClock == null) {
                    try {
                        iSystemClock = systemClockProvider.get();
                    } catch (Exception e) {
                        // ignore
                    }
                }
            }
        }
    }

}
