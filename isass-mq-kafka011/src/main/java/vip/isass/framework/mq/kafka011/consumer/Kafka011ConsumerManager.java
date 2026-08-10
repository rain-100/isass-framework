// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.mq.kafka011.consumer;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import vip.isass.framework.common.mq.MessageType;
import vip.isass.framework.mq.core.FailStrategy;
import vip.isass.framework.mq.core.MqMessage;
import vip.isass.framework.mq.core.MqMessageContext;
import vip.isass.framework.mq.core.consumer.IMqConsumer;
import vip.isass.framework.mq.core.consumer.MqConsumerManager;
import vip.isass.framework.mq.kafka011.Kafka011Const;
import vip.isass.framework.mq.kafka011.config.InstanceConfiguration;
import vip.isass.framework.mq.kafka011.config.Kafka011ConfigUtil;
import vip.isass.framework.mq.kafka011.config.Kafka011Configuration;
import vip.isass.framework.common.support.FunctionUtil;
import vip.isass.framework.common.support.JsonUtil;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * @author Rain
 */
@Slf4j
@Getter
@Setter
@Accessors(chain = true)
@Component
public class Kafka011ConsumerManager implements MqConsumerManager {

    private ExecutorService executorService;

    @Resource
    private Kafka011Configuration kafka011Configuration;

    @Autowired(required = false)
    private List<IMqConsumer> mqConsumers;

    private List<Consumer<String, String>> consumers;

    @Override
    public String getManufacturer() {
        return Kafka011Const.MANUFACTURER;
    }

    @Override
    public void subscribe() {
        if (CollUtil.isEmpty(mqConsumers)) {
            return;
        }
        consumers = new ArrayList<>(mqConsumers.size());
        List<Runnable> tasks = mqConsumers.stream()
            .filter(mc -> StrUtil.isBlank(mc.getManufacturer()) || Kafka011Const.MANUFACTURER.equals(mc.getManufacturer()))
            .map(mc -> (Runnable) () -> {
                Assert.notBlank(mc.getConsumerId());
                log.info("开始订阅事件,consumerId[{}]", mc.getConsumerId());

                String instance = MapUtil.getStr(mc.getProperties(), Kafka011Const.INSTANCE);
                InstanceConfiguration instanceConfiguration = Kafka011ConfigUtil.selectInstance(kafka011Configuration, instance);

                Properties properties = createProperties(mc, instanceConfiguration);
                KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties);
                consumer.subscribe(Collections.singletonList(parseTopic(mc, instanceConfiguration)));
                consumers.add(consumer);

                //noinspection InfiniteLoopStatement
                while (true) {
                    ConsumerRecords<String, String> records = consumer.poll(100);
                    for (ConsumerRecord<String, String> record : records) {
                        log.debug("收到mq消息：{}", record);
                        Object payload;
                        try {
                            payload = getPayload(mc, record);
                        } catch (Exception e) {
                            log.error("反序列化mq消息错误，此消息将标记为消费成功：{}，", e.getMessage(), e);
                            continue;
                        }
                        MqMessageContext mqMessageContext = new MqMessage()
                            .setTopic(record.topic())
                            .setKey(record.key())
                            .setMessageType(mc.getMessageType())
                            .setPayload(payload);
                        doConsume(mc, mqMessageContext);
                    }
                }
            })
            .collect(Collectors.toList());
        if (tasks.isEmpty()) {
            return;
        }
        executorService = Executors.newFixedThreadPool(tasks.size());
        tasks.forEach(executorService::execute);
    }

    private Properties createProperties(IMqConsumer mqConsumer, InstanceConfiguration instanceConfiguration) {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, instanceConfiguration.getServers());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, mqConsumer.getConsumerId());
        FunctionUtil.consumeIfNotNull(instanceConfiguration.getEnableAutoCommit(), p -> properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, p));
        FunctionUtil.consumeIfNotNull(instanceConfiguration.getAutoCommitIntervalMs(), p -> properties.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, p));
        FunctionUtil.consumeIfNotNull(instanceConfiguration.getAutoOffsetReset(), p -> properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, p));
        FunctionUtil.consumeIfNotNull(instanceConfiguration.getSessionTimeoutMs(), p -> properties.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, p));
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        if (CollUtil.isNotEmpty(instanceConfiguration.getProperties())) {
            properties.putAll(instanceConfiguration.getProperties());
        }
        return properties;
    }

    private void doConsume(IMqConsumer mqConsumer, MqMessageContext mqMessageContext) {
        try {
            mqConsumer.consume(mqMessageContext);
        } catch (Exception e) {
            Throwable unwrap = ExceptionUtil.unwrap(e);
            log.error("mq消费错误：{}", unwrap.getMessage(), unwrap);

            FailStrategy failStrategy = mqConsumer.getFailStrategy();
            switch (failStrategy) {
                case IGNORE:
                    log.info("忽略消费异常");
                    return;
                case RETRY:
                    // todo 实现重试
                    return;
                case RETRY_IMMEDIATELY:
                    for (int i = 0; i < mqConsumer.getImmediatelyRetryCount(); i++) {
                        log.info("正在开始第{}次重试消费。重试最大次数为{}", i + 1, mqConsumer.getImmediatelyRetryCount());
                        try {
                            mqConsumer.consume(mqMessageContext);
                            return;
                        } catch (Exception e1) {
                            log.error("重试消费错误: {}", e1.getMessage(), e1);
                        }
                    }
                    log.info("超过最大立即重试次数，将视为正常消费");
                    return;
                default:
                    log.error("未实现[{}]的失败重试策略逻辑，将视为正常消费", failStrategy);
            }
        }
    }


    private String parseTopic(IMqConsumer mqConsumer, InstanceConfiguration instanceConfiguration) {
        if (StrUtil.isNotBlank(mqConsumer.getTopic())) {
            return mqConsumer.getTopic();
        }
        switch (mqConsumer.getMessageType()) {
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
                throw new UnsupportedOperationException("未支持消息类型:" + mqConsumer.getMessageType());
        }
    }

    @SneakyThrows
    private Object getPayload(IMqConsumer mqConsumer, ConsumerRecord<String, String> record) {
        if (mqConsumer.getTypeReference() != null) {
            return JsonUtil.DEFAULT_INSTANCE.readValue(record.value(), mqConsumer.getTypeReference());
        }
        return record.value();
    }

    public void destroy() {
        if (consumers != null) {
            consumers.forEach(Consumer::close);
        }
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    @Override
    public boolean isEnable() {
        return kafka011Configuration.isEnable();
    }

}
