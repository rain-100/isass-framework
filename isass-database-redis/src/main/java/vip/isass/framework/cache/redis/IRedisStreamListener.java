// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.cache.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;

/**
 * redis
 */
public interface IRedisStreamListener<T> {

    Logger log = LoggerFactory.getLogger(IRedisStreamListener.class);

    /**
     * redis stream key
     */
    String getKey();

    /**
     * 消费者的组
     */
    String getConsumerGroup();

    /**
     * 消费者的名称
     */
    default String getConsumerName() {
        return this.hashCode() + "";
    }

    /**
     * 消费偏移量
     */
    default ReadOffset getReadOffset() {
        return ReadOffset.lastConsumed();
    }

    void onMessage(ObjectRecord<String, T> message);

}
