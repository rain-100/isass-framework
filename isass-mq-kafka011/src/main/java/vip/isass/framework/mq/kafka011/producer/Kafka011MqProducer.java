// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.mq.kafka011.producer;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import vip.isass.framework.common.mq.MessageType;
import vip.isass.framework.common.support.JsonUtil;
import vip.isass.framework.mq.core.MqMessage;
import vip.isass.framework.mq.core.producer.IMqProducer;
import vip.isass.framework.mq.kafka011.Kafka011SourceProperties;

import java.util.Properties;

@Slf4j
public class Kafka011MqProducer implements IMqProducer {

    private final Kafka011SourceProperties sourceProperties;

    private Producer<String, String> producer;

    public Kafka011MqProducer(Kafka011SourceProperties sourceProperties) {
        this.sourceProperties = sourceProperties;
    }

    public static Properties createProperties(Kafka011SourceProperties sourceProperties) {
        Properties kafkaProperties = new Properties();
        kafkaProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, sourceProperties.getServers());
        kafkaProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        kafkaProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        if (StrUtil.isNotBlank(sourceProperties.getProducerId())) {
            kafkaProperties.put(ProducerConfig.CLIENT_ID_CONFIG, sourceProperties.getProducerId());
        }
        if (sourceProperties.getProperties() != null) {
            kafkaProperties.putAll(sourceProperties.getProperties());
        }
        return kafkaProperties;
    }

    @Override
    public void init() {
        Assert.notNull(sourceProperties, "sourceProperties");
        Assert.notBlank(sourceProperties.getServers(), "servers");
        producer = new KafkaProducer<>(createProperties(sourceProperties));
    }

    @Override
    public void destroy() {
        if (producer != null) {
            producer.close();
        }
    }

    @Override
    public void send(MqMessage mqMessage) {
        Assert.notNull(mqMessage, "mqMessage");
        Assert.notNull(mqMessage.getPayload(), "payload");
        ProducerRecord<String, String> record = new ProducerRecord<>(
                resolveTopic(mqMessage), mqMessage.getKey(), getBody(mqMessage));
        try {
            producer.send(record);
        } catch (Exception e) {
            log.error("mq send failed, topic[{}], key[{}]", record.topic(), mqMessage.getKey(), e);
            throw e;
        }
    }

    @SneakyThrows
    private String getBody(MqMessage mqMessage) {
        Object payload = mqMessage.getPayload();
        return payload == null ? null : JsonUtil.NOT_NULL_INSTANCE.writeValueAsString(payload);
    }

    private String resolveTopic(MqMessage mqMessage) {
        if (StrUtil.isNotBlank(mqMessage.getTopic())) {
            return mqMessage.getTopic();
        }
        return switch (mqMessage.getMessageType()) {
            case MessageType.COMMON_MESSAGE -> sourceProperties.getCommonMessageTopic();
            case MessageType.TIMING_MESSAGE, MessageType.DELAY_MESSAGE -> sourceProperties.getTimingMessageTopic();
            case MessageType.SHARDING_SEQUENTIAL_MESSAGE -> sourceProperties.getShardingSequentialMessageTopic();
            case MessageType.GLOBAL_SEQUENTIAL_MESSAGE -> sourceProperties.getGlobalSequentialMessageTopic();
            case MessageType.TRANSACTION_MESSAGE -> throw new UnsupportedOperationException("transaction message is not supported");
            default -> sourceProperties.getDefaultTopic();
        };
    }
}
