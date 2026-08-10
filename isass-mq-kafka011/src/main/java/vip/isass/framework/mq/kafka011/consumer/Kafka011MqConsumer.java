// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.mq.kafka011.consumer;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import vip.isass.framework.mq.core.MqMessage;
import vip.isass.framework.mq.core.consumer.IMqMessageHandler;
import vip.isass.framework.mq.kafka011.Kafka011SourceProperties;

import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
public class Kafka011MqConsumer {

    private final Kafka011SourceProperties sourceProperties;

    private final List<IMqMessageHandler> mqMessageHandlers;

    private ExecutorService executorService;

    private volatile boolean running;

    public Kafka011MqConsumer(Kafka011SourceProperties sourceProperties, List<IMqMessageHandler> mqMessageHandlers) {
        this.sourceProperties = sourceProperties;
        this.mqMessageHandlers = List.copyOf(mqMessageHandlers);
    }

    public static Properties createProperties(Kafka011SourceProperties sourceProperties) {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, sourceProperties.getServers());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, sourceProperties.getConsumerGroupId());
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        if (sourceProperties.getConsumerProperties() != null) {
            properties.putAll(sourceProperties.getConsumerProperties());
        }
        return properties;
    }

    public void start() {
        Set<String> topics = mqMessageHandlers.stream()
                .map(IMqMessageHandler::getTopic)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toSet());
        if (topics.isEmpty()) {
            return;
        }
        running = true;
        executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> consumeLoop(topics));
    }

    public void stop() {
        running = false;
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    private void consumeLoop(Set<String> topics) {
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(createProperties(sourceProperties))) {
            consumer.subscribe(topics);
            while (running) {
                ConsumerRecords<String, String> records = consumer.poll(100);
                for (ConsumerRecord<String, String> record : records) {
                    dispatch(record);
                }
            }
        }
    }

    private void dispatch(ConsumerRecord<String, String> record) {
        MqMessage mqMessage = new MqMessage()
                .setTopic(record.topic())
                .setKey(record.key())
                .setPayload(record.value());
        mqMessageHandlers.stream()
                .filter(handler -> handler.getTopic().equals(record.topic()))
                .filter(handler -> "*".equals(handler.getTag()) || handler.getTag().equals(mqMessage.getTag()))
                .forEach(handler -> {
                    try {
                        handler.consume(mqMessage);
                    } catch (Exception e) {
                        Throwable unwrap = ExceptionUtil.unwrap(e);
                        log.error("mq consume failed: {}", unwrap.getMessage(), unwrap);
                    }
                });
    }
}
