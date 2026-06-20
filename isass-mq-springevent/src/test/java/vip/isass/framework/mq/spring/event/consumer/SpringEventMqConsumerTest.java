package vip.isass.framework.mq.spring.event.consumer;

import org.junit.jupiter.api.Test;
import vip.isass.framework.mq.core.MqMessage;
import vip.isass.framework.mq.core.consumer.IMqMessageHandler;
import vip.isass.framework.mq.spring.event.IsassMqEvent;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SpringEventMqConsumerTest {

    @Test
    void dispatchesMessageToMatchingTopicAndTagHandlers() {
        RecordingHandler matched = new RecordingHandler("order", "created");
        RecordingHandler otherTopic = new RecordingHandler("audit", "created");
        RecordingHandler otherTag = new RecordingHandler("order", "paid");
        SpringEventMqConsumer consumer = new SpringEventMqConsumer(List.of(matched, otherTopic, otherTag));

        MqMessage message = new MqMessage()
                .setTopic("order")
                .setTag("created")
                .setPayload("payload");
        consumer.onApplicationEvent(new IsassMqEvent(message));

        assertThat(matched.messages()).containsExactly(message);
        assertThat(otherTopic.messages()).isEmpty();
        assertThat(otherTag.messages()).isEmpty();
    }

    @Test
    void wildcardTagMatchesAnyMessageTag() {
        RecordingHandler wildcard = new RecordingHandler("order", "*");
        SpringEventMqConsumer consumer = new SpringEventMqConsumer(List.of(wildcard));

        MqMessage message = new MqMessage()
                .setTopic("order")
                .setTag("created")
                .setPayload("payload");
        consumer.onApplicationEvent(new IsassMqEvent(message));

        assertThat(wildcard.messages()).containsExactly(message);
    }

    static class RecordingHandler implements IMqMessageHandler {

        private final String topic;

        private final String tag;

        private final List<MqMessage> messages = new ArrayList<>();

        RecordingHandler(String topic, String tag) {
            this.topic = topic;
            this.tag = tag;
        }

        List<MqMessage> messages() {
            return messages;
        }

        @Override
        public String getTopic() {
            return topic;
        }

        @Override
        public String getTag() {
            return tag;
        }

        @Override
        public void consume(MqMessage mqMessage) {
            messages.add(mqMessage);
        }
    }
}
