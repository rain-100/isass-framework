// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.support.api;

/**
 * 实现接口的排序，数字越少越靠前
 */
public interface ApiOrder {

    int CACHE_SERVICE = 10;

    int LOCAL_SERVICE = 20;

    int FEIGN_SERVICE = 30;

    int CONTROLLER = 100;

    int SERVER_MANAGER = Integer.MIN_VALUE;

}
