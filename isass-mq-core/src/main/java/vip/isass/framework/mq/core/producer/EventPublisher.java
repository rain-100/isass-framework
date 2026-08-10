// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.mq.core.producer;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import vip.isass.framework.mq.core.config.DynamicMqProperties;
import vip.isass.framework.mq.core.MqMessageContext;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Rain
 */
@Slf4j
public class EventPublisher {

    private final DynamicMqProperties properties;

    private final List<ProducerManager> producerManagers;

    private static String DEFAULT_MANUFACTURER;

    private static DynamicMqProperties dynamicMqProperties;

    /**
     * key: manufacturer
     */
    private static Map<String, ProducerManager> producerManagerMap;

    private boolean running;

    public EventPublisher(DynamicMqProperties properties, List<ProducerManager> producerManagers) {
        this.properties = properties;
        dynamicMqProperties = properties;
        this.producerManagers = producerManagers == null
                ? Collections.emptyList()
                : List.copyOf(producerManagers);
    }

    /**
     * 发布事件
     *
     * @param mqMessageContext the mq message context
     */
    public static void send(@NonNull MqMessageContext mqMessageContext) {
        if (dynamicMqProperties == null || !Boolean.TRUE.equals(dynamicMqProperties.getEnabled())) {
            return;
        }
        Assert.notNull(producerManagerMap, "生产者管理器未初始化，或者没有启用mq，mq发送失败");

        // 找实现厂商
        if (StrUtil.isBlank(mqMessageContext.getManufacturer())) {
            mqMessageContext.setManufacturer(DEFAULT_MANUFACTURER);
        }
        if (StrUtil.isBlank(mqMessageContext.getManufacturer())) {
            mqMessageContext.setManufacturer(producerManagerMap.entrySet().iterator().next().getKey());
        }

        ProducerManager producerManager = producerManagerMap.get(mqMessageContext.getManufacturer());
        Assert.notNull(producerManager, "厂商[{}]未启用或未配置 producerManager, mq 发送失败", mqMessageContext.getManufacturer());
        producerManager.send(mqMessageContext);
    }

    public void start() {
        running = true;
        if (Boolean.TRUE.equals(properties.getEnabled())) {
            log.info("init mq producer manager");
        } else {
            log.info("mq is disable, skip to init EventPublisher");
            return;
        }

        if (CollUtil.isEmpty(producerManagers)) {
            log.info("can not find any ProducerManager, will skip it");
            return;
        }

        producerManagers.stream()
            .filter(s -> StrUtil.isBlank(s.manufacturer()))
            .findFirst()
            .ifPresent(s -> {
                throw new IllegalArgumentException(s.getClass().toGenericString() + " 的 manufacturer 不能为空");
            });

        producerManagerMap = producerManagers
            .stream()
            .filter(s -> StrUtil.isNotBlank(s.manufacturer()))
            .filter(ProducerManager::isEnable)
            .peek(ProducerManager::init)
            .collect(Collectors.toMap(ProducerManager::manufacturer, Function.identity()));

        DEFAULT_MANUFACTURER = properties.getPrimary();
    }

    public void stop() {
        if (CollUtil.isNotEmpty(producerManagers)) {
            producerManagers.forEach(ProducerManager::destroy);
        }
        running = false;
    }

    public boolean isRunning() {
        return running;
    }

}
