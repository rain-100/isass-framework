// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.support;

import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * CostUtil 测试类
 *
 * @author Rain
 */
public class CostUtilTest {

    public static void main(String[] args) {
        CostUtil.compute(5, 10, (t) -> {
            Integer one = 1;
            return Boolean.TRUE;
        });
    }
}