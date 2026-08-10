// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.support;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;

/**
 * @author Rain
 */
@Slf4j
public class RetryUtil {

    public static void retryOrSleep1Second(Supplier<Boolean> supplier, String errorMsg) {
        retryOrSleep(supplier, errorMsg, 1, TimeUnit.SECONDS);
    }

    public static void retryOrSleep(Supplier<Boolean> supplier, String errorMsg, int sleep, TimeUnit timeUnit) {
        while (true) {
            try {
                if (supplier.get()) {
                    break;
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                try {
                    timeUnit.sleep(sleep);
                } catch (InterruptedException e1) {
                    log.error("{} {}", errorMsg, e.getMessage(), e1);
                }
            }
        }
    }

    /**
     * @param supplier supplier
     * @param count    重试次数
     */
    public static void retry(Supplier<?> supplier, int count) {
        LongAdder adder = new LongAdder();
        while (true) {
            try {
                adder.increment();
                int i = adder.intValue();
                log.debug("开始执行retry任务。正常尝试第{}次，共需尝试{}次", i, count);
                supplier.get();
                break;
            } catch (Exception e) {
                int i = adder.intValue();
                log.error("retry错误，重试进度：{}/{}。{}", i, count, e.getMessage(), e);
                if (i >= count) {
                    break;
                }
            }
        }
    }

}
