// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.sequence.impl;

import vip.isass.framework.common.sequence.Sequence;

import java.util.UUID;

/**
 * @author rain
 */
public class UuidSequence implements Sequence<String> {

    @Override
    public String next() {
        return get();
    }

    public static String get() {
        return UUID.randomUUID().toString();
    }

    
    @Override
    public boolean support(Class<?> clazz) {
        return clazz == String.class;
    }

}
