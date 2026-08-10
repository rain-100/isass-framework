// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.mq.kafka011.producer;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import vip.isass.framework.common.mq.MessageType;
import vip.isass.framework.mq.core.MqMessageContext;
import vip.isass.framework.mq.core.producer.MqProducer;
import vip.isass.framework.mq.kafka011.config.InstanceConfiguration;
import vip.isass.framework.mq.kafka011.config.ProducerConfiguration;
import vip.isass.framework.common.support.JsonUtil;

import java.util.Properties;
import java.util.concurrent.Future;

/**
 * @author Rain
 */
@Slf4j
@Accessors(chain = true)
public class Kafka011Producer implements MqProducer {

    @Getter
    @Setter
    private InstanceConfiguration instanceConfiguration;

    @Getter
    @Setter
    private ProducerConfiguration producerConfiguration;

    private Producer<String, String> producer;

    //    private OrderProducer orderProducer;

    @Override
    public void send(MqMessageContext mqMessageContext) {
        Assert.notNull(mqMessageContext);
        Assert.notBlank(mqMessageContext.getTopic());
        //        Assert.notBlank(mqMessageContext.getTag());
        Assert.notNull(mqMessageContext.getPayload());
        //
        ProducerRecord<String, String> record = new ProducerRecord<>(
            getTopic(mqMessageContext), "", getBody(mqMessageContext));

        try {
            Future<RecordMetadata> send = producer.send(record);
        } catch (Exception e) {
            log.error("mq发送失败,topic[{}], messageKey[{}]", mqMessageContext.getTopic(), mqMessageContext.getKey());
            throw e;
        }
    }

    @SneakyThrows
    private String getBody(MqMessageContext mqMessageContext) {
        String body;
        Object payload = mqMessageContext.getPayload();
        if (payload == null) {
            body = null;
        } else {
            body = JsonUtil.NOT_NULL_INSTANCE.writeValueAsString(payload);
        }
        return body;
    }

    private String getTopic(MqMessageContext mqMessageContext) {
        if (StrUtil.isNotBlank(mqMessageContext.getTopic())) {
            return mqMessageContext.getTopic();
        }
        int messageType = mqMessageContext.getMessageType();
        switch (messageType) {
            case MessageType.COMMON_MESSAGE:
                return instanceConfiguration.getCommonMessageTopic();
            case MessageType.TIMING_MESSAGE:
            case MessageType.DELAY_MESSAGE:
                return instanceConfiguration.getTimingMessageTopic();
            case MessageType.TRANSACTION_MESSAGE:
                throw new UnsupportedOperationException("未支持事务消息");
            case MessageType.SHARDING_SEQUENTIAL_MESSAGE:
                return instanceConfiguration.getShardingSequentialMessageTopic();
            case MessageType.GLOBAL_SEQUENTIAL_MESSAGE:
                return instanceConfiguration.getGlobalSequentialMessageTopic();
            default:
                throw new UnsupportedOperationException("未支持消息类型:" + messageType);
        }
    }

    @Override
    public Kafka011Producer init() {
        Assert.notNull(instanceConfiguration);
        Assert.notBlank(instanceConfiguration.getServers());
        Assert.notNull(producerConfiguration);
        Assert.notBlank(producerConfiguration.getProducerId());

        Properties kafkaProps = new Properties();
        kafkaProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, instanceConfiguration.getServers());
        kafkaProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        kafkaProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        if (CollUtil.isNotEmpty(producerConfiguration.getProperties())) {
            kafkaProps.putAll(producerConfiguration.getProperties());
        }
        producer = new KafkaProducer<>(kafkaProps);
        return this;
    }

    @Override
    public void destroy() {
        if (producer != null) {
            producer.close();
        }
    }

}
