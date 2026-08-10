// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.sequence.impl;

import cn.hutool.core.util.RandomUtil;
import lombok.extern.slf4j.Slf4j;
import vip.isass.framework.common.sequence.Sequence;

import java.util.function.Supplier;

/**
 * @author rain
 */
@Slf4j
public class LongSequence implements Sequence<Long> {

    private static volatile Sequence<Long> sequence;
    private static volatile Supplier<Sequence<Long>> sequenceProvider = () -> null;

    public static void setSequence(Sequence<Long> sequence) {
        LongSequence.sequence = sequence;
        LongSequence.sequenceProvider = () -> sequence;
    }

    public static void setSequenceProvider(Supplier<Sequence<Long>> sequenceProvider) {
        LongSequence.sequence = null;
        LongSequence.sequenceProvider = sequenceProvider == null ? () -> null : sequenceProvider;
    }

    @Override
    public Long next() {
        return get();
    }

    @SuppressWarnings("unchecked")
    public static Long get() {
        if (sequence == null) {
            synchronized (LongSequence.class) {
                if (sequence == null) {
                    try {
                        sequence = sequenceProvider.get();
                    } catch (Exception e) {
                        // ignore
                    }
                }
            }
        }
        return sequence == null ? RandomUtil.randomLong(1000000000, Long.MAX_VALUE) : sequence.next();
    }

    @Override
    public boolean support(Class<?> clazz) {
        return clazz == Long.class;
    }

}
