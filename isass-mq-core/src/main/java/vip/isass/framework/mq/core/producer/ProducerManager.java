// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.mq.core.producer;

import vip.isass.framework.mq.core.MqMessageContext;

/**
 * @author Rain
 */
public interface ProducerManager {

    /**
     * 提供实现的厂商
     * 例如：阿里云rocketmq、kafaka、rabbitmq
     *
     * @return manufacturer name
     */
    String manufacturer();

    void init();

    void destroy();

    boolean isEnable();

    void send(MqMessageContext mqMessageContext);

}
