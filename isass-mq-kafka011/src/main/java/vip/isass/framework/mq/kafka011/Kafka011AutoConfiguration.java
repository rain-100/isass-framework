// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.mq.kafka011;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;
import vip.isass.framework.mq.kafka011.config.Kafka011Configuration;
import vip.isass.framework.mq.kafka011.consumer.Kafka011ConsumerManager;
import vip.isass.framework.mq.kafka011.producer.Kafka011ProducerAutoConfiguration;
import vip.isass.framework.mq.kafka011.producer.Kafka011ProducerManager;

@AutoConfiguration
@Import({
        Kafka011Configuration.class,
        Kafka011ConsumerManager.class,
        Kafka011ProducerAutoConfiguration.class,
        Kafka011ProducerManager.class,
        Kafka011MqFactory.class
})
public class Kafka011AutoConfiguration {

}
