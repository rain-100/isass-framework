// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.mq.kafka011;

import org.springframework.stereotype.Component;
import vip.isass.framework.mq.core.IMqFactory;
import vip.isass.framework.mq.core.config.MqSourceProperties;
import vip.isass.framework.mq.core.consumer.IMqConsumerContainer;
import vip.isass.framework.mq.core.consumer.IMqMessageHandler;
import vip.isass.framework.mq.core.producer.IMqProducer;
import vip.isass.framework.mq.kafka011.consumer.Kafka011MqConsumer;
import vip.isass.framework.mq.kafka011.producer.Kafka011MqProducer;

import java.util.List;

@Component
public class Kafka011MqFactory implements IMqFactory {

    @Override
    public String getType() {
        return "kafka011";
    }

    @Override
    public Class<? extends MqSourceProperties> getPropertiesType() {
        return Kafka011SourceProperties.class;
    }

    @Override
    public IMqConsumerContainer createMqConsumer(MqSourceProperties sourceProperties, List<IMqMessageHandler> mqMessageHandlers) {
        if (mqMessageHandlers == null || mqMessageHandlers.isEmpty()) {
            return null;
        }
        Kafka011MqConsumer consumer = new Kafka011MqConsumer(toKafkaSourceProperties(sourceProperties), mqMessageHandlers);
        consumer.start();
        return consumer::stop;
    }

    @Override
    public IMqProducer createMqProducer(MqSourceProperties sourceProperties) {
        return new Kafka011MqProducer(toKafkaSourceProperties(sourceProperties));
    }

    private Kafka011SourceProperties toKafkaSourceProperties(MqSourceProperties sourceProperties) {
        if (sourceProperties instanceof Kafka011SourceProperties kafka011SourceProperties) {
            return kafka011SourceProperties;
        }
        Kafka011SourceProperties kafka011SourceProperties = new Kafka011SourceProperties();
        kafka011SourceProperties.setName(sourceProperties.getName());
        kafka011SourceProperties.setEnabled(sourceProperties.getEnabled());
        kafka011SourceProperties.setType(sourceProperties.getType());
        if (sourceProperties.getOptions() != null) {
            kafka011SourceProperties.setServers(option(sourceProperties, "servers"));
            kafka011SourceProperties.setProducerId(option(sourceProperties, "producer-id"));
            kafka011SourceProperties.setConsumerGroupId(option(sourceProperties, "consumer-group-id"));
            kafka011SourceProperties.setDefaultTopic(option(sourceProperties, "default-topic"));
            kafka011SourceProperties.setCommonMessageTopic(option(sourceProperties, "common-message-topic"));
            kafka011SourceProperties.setShardingSequentialMessageTopic(option(sourceProperties, "sharding-sequential-message-topic"));
            kafka011SourceProperties.setGlobalSequentialMessageTopic(option(sourceProperties, "global-sequential-message-topic"));
            kafka011SourceProperties.setTimingMessageTopic(option(sourceProperties, "timing-message-topic"));
        }
        return kafka011SourceProperties;
    }

    private String option(MqSourceProperties sourceProperties, String key) {
        Object value = sourceProperties.getOptions().get(key);
        return value == null ? null : String.valueOf(value);
    }
}
