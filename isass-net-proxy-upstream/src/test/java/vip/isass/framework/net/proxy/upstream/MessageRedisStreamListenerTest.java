// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.proxy.upstream;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import vip.isass.framework.net.core.NetRedisKey;
import vip.isass.framework.net.core.handler.manager.IEventManager;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MessageRedisStreamListenerTest {

    @Test
    void destroyDeletesConsumerWithoutMutatingRedissonStaticState() {
        @SuppressWarnings("unchecked")
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        @SuppressWarnings("unchecked")
        StreamOperations<String, Object, Object> streamOperations = mock(StreamOperations.class);
        when(redisTemplate.opsForStream()).thenReturn(streamOperations);

        MessageRedisStreamListener listener = new MessageRedisStreamListener(
                mock(IEventManager.class), redisTemplate, "im-service");

        listener.destroy();

        verify(streamOperations).deleteConsumer(eq(NetRedisKey.REDIS_STREAM_PREFIX_KEY + "im-service"), any());
    }
}
