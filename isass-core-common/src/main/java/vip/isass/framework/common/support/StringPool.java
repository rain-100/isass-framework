// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.support;

import com.google.common.collect.Interner;
import com.google.common.collect.Interners;

public class StringPool {

    private static final Interner<String> STRING_POOL = Interners.newWeakInterner();

    public static final String intern(String str) {
        return STRING_POOL.intern(str);
    }

}
