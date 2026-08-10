// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.mq.core;

import vip.isass.framework.mq.core.config.MqSourceProperties;
import vip.isass.framework.mq.core.consumer.IMqConsumerContainer;
import vip.isass.framework.mq.core.consumer.IMqMessageHandler;
import vip.isass.framework.mq.core.producer.IMqProducer;

import java.util.List;

public interface IMqFactory {

    String getType();

    Class<? extends MqSourceProperties> getPropertiesType();

    IMqConsumerContainer createMqConsumer(MqSourceProperties sourceProperties, List<IMqMessageHandler> mqMessageHandlers);

    IMqProducer createMqProducer(MqSourceProperties sourceProperties);

    default boolean validate(MqSourceProperties sourceProperties) {
        return sourceProperties != null && Boolean.TRUE.equals(sourceProperties.getEnabled());
    }
}
