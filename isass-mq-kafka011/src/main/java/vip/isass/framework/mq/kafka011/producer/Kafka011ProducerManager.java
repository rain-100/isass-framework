// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.mq.kafka011.producer;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Component;
import vip.isass.framework.common.mq.MessageType;
import vip.isass.framework.mq.core.MqMessageContext;
import vip.isass.framework.mq.core.producer.MqProducer;
import vip.isass.framework.mq.core.producer.ProducerManager;
import vip.isass.framework.mq.kafka011.Kafka011Const;
import vip.isass.framework.mq.kafka011.config.InstanceConfiguration;
import vip.isass.framework.mq.kafka011.config.Kafka011ConfigUtil;
import vip.isass.framework.mq.kafka011.config.Kafka011Configuration;
import vip.isass.framework.mq.kafka011.config.ProducerConfiguration;

import jakarta.annotation.Resource;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Rain
 */
@Component
public class Kafka011ProducerManager implements ProducerManager {

    @Resource
    private Kafka011Configuration kafka011Configuration;

    @Resource
    private Kafka011ProducerAutoConfiguration kafka011ProducerAutoConfiguration;

    private Map<String, Kafka011Producer> producerGroupByProducerId = Collections.emptyMap();

    private MqProducer selectProducer(final MqMessageContext mqMessageContext) {
        final InstanceConfiguration instanceConfiguration = Kafka011ConfigUtil.selectInstance(
            kafka011Configuration, mqMessageContext.getStringProperty(Kafka011Const.INSTANCE));
        mqMessageContext.setProperty(Kafka011Const.INSTANCE, instanceConfiguration.getInstanceName());

        ProducerConfiguration producerConfiguration = Kafka011ConfigUtil.selectProducer(
            kafka011Configuration, instanceConfiguration, mqMessageContext.getStringProperty(Kafka011Const.PRODUCER_ID));

        // 设置topic
        if (StrUtil.isBlank(mqMessageContext.getTopic())) {
            switch (mqMessageContext.getMessageType()) {
                case MessageType.COMMON_MESSAGE:
                    mqMessageContext.setTopic(instanceConfiguration.getCommonMessageTopic());
                    break;
                case MessageType.TIMING_MESSAGE:
                    throw new UnsupportedOperationException("未支持TIMING_MESSAGE");
                case MessageType.DELAY_MESSAGE:
                    throw new UnsupportedOperationException("未支持DELAY_MESSAGE");
                case MessageType.TRANSACTION_MESSAGE:
                    throw new UnsupportedOperationException("未支持TRANSACTION_MESSAGE");
                case MessageType.SHARDING_SEQUENTIAL_MESSAGE:
                    mqMessageContext.setTopic(instanceConfiguration.getShardingSequentialMessageTopic());
                    break;
                case MessageType.GLOBAL_SEQUENTIAL_MESSAGE:
                    mqMessageContext.setTopic(instanceConfiguration.getGlobalSequentialMessageTopic());
                    break;
                default:
                    throw new UnsupportedOperationException("未支持" + mqMessageContext.getMessageType());
            }
        }

        return producerGroupByProducerId.get(producerConfiguration.getProducerId());
    }

    @Override
    public String manufacturer() {
        return Kafka011Const.MANUFACTURER;
    }

    @Override
    public void destroy() {
        producerGroupByProducerId.values().forEach(Kafka011Producer::destroy);
    }

    @Override
    public boolean isEnable() {
        return kafka011Configuration.isEnable();
    }

    @Override
    public void send(MqMessageContext mqMessageContext) {
        MqProducer mqProducer = selectProducer(mqMessageContext);
        Assert.notNull(mqProducer, "未找到mq生产者，mq发送失败");
        mqProducer.send(mqMessageContext);
    }

    @Override
    public void init() {
        if (CollUtil.isEmpty(kafka011ProducerAutoConfiguration.getProducers())) {
            return;
        }

        producerGroupByProducerId = kafka011ProducerAutoConfiguration.getProducers()
            .stream()
            .collect(Collectors.toMap((o) -> o.getProducerConfiguration().getProducerId(), Function.identity()));
    }

}
