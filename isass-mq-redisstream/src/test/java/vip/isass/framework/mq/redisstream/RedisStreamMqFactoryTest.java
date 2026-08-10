// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.mq.redisstream;

import org.junit.jupiter.api.Test;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.stream.StreamAddArgs;
import vip.isass.framework.mq.core.MqMessage;
import vip.isass.framework.mq.core.config.MqSourceProperties;
import vip.isass.framework.mq.core.producer.IMqProducer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisStreamMqFactoryTest {

    @Test
    void exposesRedisStreamType() {
        assertThat(new RedisStreamMqFactory(mock(RedissonClient.class)).getType()).isEqualTo("redisstream");
    }

    @Test
    @SuppressWarnings("unchecked")
    void createsProducerThatAddsMqMessageToRedissonStream() {
        RedissonClient redissonClient = mock(RedissonClient.class);
        RStream<String, Object> stream = mock(RStream.class);
        when(redissonClient.<String, Object>getStream("test.topic")).thenReturn(stream);
        RedisStreamMqFactory factory = new RedisStreamMqFactory(redissonClient);
        IMqProducer producer = factory.createMqProducer(source());

        producer.send(new MqMessage().setTopic("test.topic").setTag("ttt").setPayload("hello"));

        verify(stream).add(any(StreamAddArgs.class));
    }

    private MqSourceProperties source() {
        MqSourceProperties properties = new MqSourceProperties();
        properties.setName("redis-stream");
        properties.setType("redisstream");
        properties.setEnabled(true);
        return properties;
    }
}
