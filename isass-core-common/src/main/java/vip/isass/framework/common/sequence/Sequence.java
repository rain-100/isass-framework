// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.sequence;


import vip.isass.framework.common.support.Support;

/**
 * 获取序列
 *
 * @param <T> 序列类型
 * @author Rain
 */
public interface Sequence<T> extends Support {

    /**
     * @return 序列
     */
    T next();

}
