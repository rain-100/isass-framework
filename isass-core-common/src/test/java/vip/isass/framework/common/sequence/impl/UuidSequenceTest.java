// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.sequence.impl;

import vip.isass.framework.common.sequence.Sequence;

import java.util.UUID;

/**
 * UuidSequence 测试类
 *
 * @author rain
 */
public class UuidSequenceTest implements Sequence<String> {

    @Override
    public String next() {
        return get();
    }

    public static String get() {
        return UUID.randomUUID().toString();
    }

    public static void main(String[] args) {
        for (int i = 0; i < 100; i++) {
            System.out.println(UuidSequenceTest.get());
        }
    }

    @Override
    public boolean support(Class<?> clazz) {
        return clazz == String.class;
    }

}