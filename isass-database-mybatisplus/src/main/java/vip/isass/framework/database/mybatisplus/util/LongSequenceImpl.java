// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.database.mybatisplus.util;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import vip.isass.framework.common.sequence.Sequence;

/**
 * @author Rain
 */
public class LongSequenceImpl implements Sequence<Long> {

    @Override
    public Long next() {
        return get();
    }

    public static Long get() {
        return IdWorker.getId();
    }

    @Override
    public boolean support(Class<?> clazz) {
        return clazz == Long.class;
    }

}
