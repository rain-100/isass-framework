package vip.isass.framework.mq.redispubsub;

import org.junit.jupiter.api.Test;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.api.listener.MessageListener;
import vip.isass.framework.mq.core.MqMessage;
import vip.isass.framework.mq.core.config.MqSourceProperties;
import vip.isass.framework.mq.core.consumer.IMqMessageHandler;
import vip.isass.framework.mq.core.producer.IMqProducer;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisPubSubMqFactoryTest {

    @Test
    void createsProducerThatPublishesMqMessageToRedissonTopic() {
        RedissonClient redissonClient = mock(RedissonClient.class);
        RTopic topic = mock(RTopic.class);
        when(redissonClient.getTopic("test.topic")).thenReturn(topic);
        RedisPubSubMqFactory factory = new RedisPubSubMqFactory(redissonClient);
        IMqProducer producer = factory.createMqProducer(source());

        MqMessage message = new MqMessage().setTopic("test.topic").setTag("ttt").setPayload("hello");
        producer.send(message);

        verify(topic).publish(message);
    }

    @Test
    @SuppressWarnings("unchecked")
    void registersHandlerAsRedissonTopicListener() {
        RedissonClient redissonClient = mock(RedissonClient.class);
        RTopic topic = mock(RTopic.class);
        when(redissonClient.getTopic("test.topic")).thenReturn(topic);
        RecordingHandler handler = new RecordingHandler();
        RedisPubSubMqFactory factory = new RedisPubSubMqFactory(redissonClient);

        factory.createMqConsumer(source(), List.of(handler));

        org.mockito.ArgumentCaptor<MessageListener<MqMessage>> captor =
                org.mockito.ArgumentCaptor.forClass(MessageListener.class);
        verify(topic).addListener(eq(MqMessage.class), captor.capture());
        MqMessage message = new MqMessage().setTopic("test.topic").setTag("ttt").setPayload("hello");
        captor.getValue().onMessage("test.topic", message);

        assertThat(handler.messages).containsExactly(message);
    }

    private MqSourceProperties source() {
        MqSourceProperties properties = new MqSourceProperties();
        properties.setName("redis-pubsub");
        properties.setType("redispubsub");
        properties.setEnabled(true);
        return properties;
    }

    static class RecordingHandler implements IMqMessageHandler {

        private final List<MqMessage> messages = new ArrayList<>();

        @Override
        public String getTopic() {
            return "test.topic";
        }

        @Override
        public String getTag() {
            return "ttt";
        }

        @Override
        public void consume(MqMessage mqMessage) {
            messages.add(mqMessage);
        }
    }
}
