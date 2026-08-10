// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.mq.kafka011.config;

import cn.hutool.core.util.StrUtil;

/**
 * @author Rain
 */
public class Kafka011ConfigUtil {

    /**
     * 实例选择的优先级：
     * 1：用户传参的实例
     * 2：配置默认的实例
     * 3：配置排第一的实例
     *
     * @param kafka011Configuration region configuration
     * @param instance              用户传参的实例
     * @return instance configuration
     */
    public static InstanceConfiguration selectInstance(Kafka011Configuration kafka011Configuration, String instance) {
        InstanceConfiguration instanceConfiguration = null;

        // 用户传参的实例
        if (StrUtil.isNotBlank(instance)) {
            instanceConfiguration = kafka011Configuration.getInstances().stream()
                .filter(i -> i.getInstanceName().equals(instance))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(StrUtil.format(
                    "kafka011没有配置实例[{}]",
                    instance)));
        }

        // 配置默认的实例
        if (instanceConfiguration == null && StrUtil.isNotBlank(kafka011Configuration.getDefaultInstance())) {
            kafka011Configuration.getInstances().stream()
                .filter(i -> i.getInstanceName().equals(kafka011Configuration.getDefaultInstance()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(StrUtil.format(
                    "kafka011默认实例配置错误",
                    kafka011Configuration)));
        }

        // 配置排第一的实例
        if (instanceConfiguration == null) {
            instanceConfiguration = kafka011Configuration.getInstances().iterator().next();
        }

        if (instanceConfiguration == null) {
            throw new IllegalArgumentException(StrUtil.format("kafka011没有配置实例"));
        }

        return instanceConfiguration;
    }

    public static ProducerConfiguration selectProducer(Kafka011Configuration kafka011Configuration,
                                                       InstanceConfiguration instanceConfiguration,
                                                       String producer) {
        ProducerConfiguration producerConfiguration = null;

        // 用户传参的producer
        if (StrUtil.isNotBlank(producer)) {
            producerConfiguration = instanceConfiguration.getProducers().stream()
                .filter(i -> i.getProducerId().equals(producer))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(StrUtil.format(
                    "kafka011 instance[{}]没有配置producer[{}]",
                    instanceConfiguration.getInstanceName(),
                    producer)));
        }

        // 配置默认的producer
        if (producerConfiguration == null && StrUtil.isNotBlank(instanceConfiguration.getDefaultProducer())) {
            producerConfiguration = instanceConfiguration.getProducers().stream()
                .filter(p -> p.getProducerId().equals(instanceConfiguration.getDefaultProducer()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(StrUtil.format(
                    "kafka011 instance[{}]默认producer配置错误",
                    instanceConfiguration.getInstanceName())));
        }

        // 配置排第一的producer
        if (producerConfiguration == null) {
            producerConfiguration = instanceConfiguration.getProducers().iterator().next();
        }

        if (producerConfiguration == null) {
            throw new IllegalArgumentException(StrUtil.format(
                "kafka011 instance[{}]没有配置producer",
                instanceConfiguration.getInstanceName()));
        }
        return producerConfiguration;
    }
}
