// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.mq.core.consumer;

import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;
import vip.isass.framework.mq.core.config.DynamicMqProperties;

import java.util.Collections;
import java.util.List;

/**
 * 查找所有订阅者，进行订阅
 *
 * @author Rain
 */
@Slf4j
public class MqConsumerAutoConfiguration {

    private final DynamicMqProperties properties;

    private final List<MqConsumerManager> mqConsumerManagers;

    private boolean running;

    public MqConsumerAutoConfiguration(DynamicMqProperties properties, List<MqConsumerManager> mqConsumerManagers) {
        this.properties = properties;
        this.mqConsumerManagers = mqConsumerManagers == null
                ? Collections.emptyList()
                : List.copyOf(mqConsumerManagers);
    }

    public void start() {
        running = true;
        if (Boolean.TRUE.equals(properties.getEnabled())) {
            log.info("init mq consumer manager");
        } else {
            log.info("isass event system is disable, will skip it");
            return;
        }

        if (CollUtil.isEmpty(mqConsumerManagers)) {
            return;
        }
        mqConsumerManagers
            .stream()
            .filter(MqConsumerManager::isEnable)
            .forEach(MqConsumerManager::subscribe);
    }

    public void stop() {
        if (CollUtil.isNotEmpty(mqConsumerManagers)) {
            mqConsumerManagers.forEach(MqConsumerManager::destroy);
        }
        running = false;
    }

    public boolean isRunning() {
        return running;
    }

}
