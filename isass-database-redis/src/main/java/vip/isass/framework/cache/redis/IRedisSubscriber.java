// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.cache.redis;

import cn.hutool.core.util.TypeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.Topic;
import vip.isass.framework.common.converter.ConvertUtil;

import jakarta.annotation.Nonnull;
import java.lang.reflect.Type;

/**
 * redis 消息订阅者。
 * 使用 redis pubsub 机制
 *
 * @author rain
 */
public interface IRedisSubscriber<T> extends MessageListener {

    Logger log = LoggerFactory.getLogger(IRedisSubscriber.class);

    /**
     * 订阅的主题，支持匹配符
     *
     * @return 监听的 topic
     */
    Topic topic();

    /**
     * Callback for processing received objects through Redis.
     *
     * @param message message must not be {@literal null}.
     * @param pattern pattern matching the channel (if specified) - can be {@literal null}.
     */
    @Override
    default void onMessage(@Nonnull Message message, byte[] pattern) {
        T convertedPayload;
        try {
            Type actualType = TypeUtil.toParameterizedType(this.getClass()).getActualTypeArguments()[0];
            convertedPayload = ConvertUtil.convert(actualType, message.getBody());
            onMessage(convertedPayload, message, pattern);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

    }

    /**
     * 执行的回调逻辑
     *
     * @param payload payload
     * @param message 原始 redis 消息
     * @param pattern pattern matching the channel (if specified) - can be null.
     */
    void onMessage(T payload, Message message, byte[] pattern);

}
