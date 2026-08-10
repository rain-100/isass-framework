// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.mq.kafka011.producer;

import cn.hutool.core.collection.CollUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import vip.isass.framework.mq.kafka011.config.InstanceConfiguration;
import vip.isass.framework.mq.kafka011.config.Kafka011Configuration;
import vip.isass.framework.mq.kafka011.config.ProducerConfiguration;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Rain
 */
@Slf4j
@Configuration
public class Kafka011ProducerAutoConfiguration {

    @Resource
    private Kafka011Configuration kafka011Configuration;

    @Getter
    private List<Kafka011Producer> producers;

    @PostConstruct
    public void init() {
        if (!kafka011Configuration.isEnable()) {
            return;
        }
        producers = new ArrayList<>();
        for (InstanceConfiguration instanceConfiguration : kafka011Configuration.getInstances()) {
            for (ProducerConfiguration producerConfiguration : instanceConfiguration.getProducers()) {
                producers.add(new Kafka011Producer()
                    .setInstanceConfiguration(instanceConfiguration)
                    .setProducerConfiguration(producerConfiguration)
                    .init());
            }
        }
    }

    @PreDestroy
    public void destroy() {
        if (CollUtil.isEmpty(producers)) {
            return;
        }
        producers.stream()
            .filter(Objects::nonNull)
            .forEach(Kafka011Producer::destroy);
    }

}
