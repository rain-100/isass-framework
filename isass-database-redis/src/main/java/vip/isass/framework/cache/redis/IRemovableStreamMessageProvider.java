// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.cache.redis;

import java.util.Collection;

/**
 * 可以删除的 redis stream message
 */
public interface IRemovableStreamMessageProvider {

    /**
     * redis stream key
     */
    Collection<String> getKeys();

}
