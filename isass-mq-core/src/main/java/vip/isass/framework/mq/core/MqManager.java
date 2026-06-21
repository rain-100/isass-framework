package vip.isass.framework.mq.core;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import vip.isass.framework.mq.core.config.DynamicMqProperties;
import vip.isass.framework.mq.core.config.MqSourceProperties;
import vip.isass.framework.mq.core.consumer.IMqConsumerContainer;
import vip.isass.framework.mq.core.consumer.IMqMessageHandler;
import vip.isass.framework.mq.core.producer.IMqProducer;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class MqManager {

    private final DynamicMqProperties properties;

    private final List<IMqMessageHandler> mqMessageHandlers;

    private final List<IMqFactory> mqFactories;

    private final Map<String, IMqProducer> producerMap = new LinkedHashMap<>();

    private final Map<String, IMqConsumerContainer> consumerMap = new LinkedHashMap<>();

    private boolean running;

    public MqManager(DynamicMqProperties properties, List<IMqMessageHandler> mqMessageHandlers) {
        this(properties, mqMessageHandlers, List.of());
    }

    public MqManager(DynamicMqProperties properties, List<IMqMessageHandler> mqMessageHandlers,
                     List<IMqFactory> mqFactories) {
        this.properties = properties;
        this.mqMessageHandlers = mqMessageHandlers == null
                ? Collections.emptyList()
                : List.copyOf(mqMessageHandlers);
        this.mqFactories = mqFactories == null
                ? Collections.emptyList()
                : List.copyOf(mqFactories);
        MqPublisher.setMqManager(this);
    }

    public void start() {
        if (!Boolean.TRUE.equals(properties.getEnabled())) {
            log.info("mq is disabled, skip to init mq sources");
            running = true;
            return;
        }
        if (CollUtil.isEmpty(properties.getSources())) {
            throw new IllegalStateException("mq is enabled, but no mq sources configured");
        }
        producerMap.clear();
        consumerMap.clear();
        properties.getSources().forEach(this::startSource);
        if (!producerMap.containsKey(properties.getPrimary())) {
            throw new IllegalStateException("primary mq source [" + properties.getPrimary() + "] is not enabled or configured");
        }
        running = true;
    }

    private void startSource(String sourceName, MqSourceProperties sourceProperties) {
        if (sourceProperties == null || !Boolean.TRUE.equals(sourceProperties.getEnabled())) {
            return;
        }
        if (StrUtil.isBlank(sourceProperties.getName())) {
            sourceProperties.setName(sourceName);
        }
        IMqFactory factory = createFactory(sourceName, sourceProperties);
        if (!factory.validate(sourceProperties)) {
            return;
        }
        List<IMqMessageHandler> sourceHandlers = mqMessageHandlers.stream()
                .filter(handler -> StrUtil.isBlank(handler.getSource()) || sourceName.equals(handler.getSource()))
                .collect(Collectors.toList());
        IMqConsumerContainer consumer = factory.createMqConsumer(sourceProperties, sourceHandlers);
        if (consumer != null) {
            consumerMap.put(sourceName, consumer);
        }
        IMqProducer producer = factory.createMqProducer(sourceProperties);
        if (producer != null) {
            producer.init();
            producerMap.put(sourceName, producer);
        }
    }

    private IMqFactory createFactory(String sourceName, MqSourceProperties sourceProperties) {
        if (StrUtil.isBlank(sourceProperties.getType())) {
            throw new IllegalArgumentException("mq source [" + sourceName + "] type can not be blank");
        }
        if (CollUtil.isEmpty(mqFactories)) {
            throw new IllegalArgumentException("mq source [" + sourceName + "] has no available mq factory");
        }

        String type = sourceProperties.getType();
        List<IMqFactory> matchedFactories = mqFactories.stream()
                .filter(factory -> type.equals(factory.getType()))
                .collect(Collectors.toList());
        if (matchedFactories.size() == 1) {
            return matchedFactories.get(0);
        }
        if (matchedFactories.size() > 1) {
            throw new IllegalArgumentException("mq source [" + sourceName + "] matched multiple mq factories by type [" + type + "]");
        }
        throw new IllegalArgumentException("mq source [" + sourceName + "] can not match mq factory by type [" + type + "]");
    }

    public void send(MqMessage mqMessage) {
        send(properties.getPrimary(), mqMessage);
    }

    public void send(String sourceName, MqMessage mqMessage) {
        IMqProducer producer = producerMap.get(sourceName);
        if (producer == null) {
            throw new IllegalArgumentException("mq source [" + sourceName + "] is not enabled or configured");
        }
        producer.send(mqMessage);
    }

    public void stop() {
        consumerMap.values().forEach(IMqConsumerContainer::destroy);
        consumerMap.clear();
        producerMap.values().forEach(IMqProducer::destroy);
        producerMap.clear();
        running = false;
    }

    public boolean isRunning() {
        return running;
    }
}
