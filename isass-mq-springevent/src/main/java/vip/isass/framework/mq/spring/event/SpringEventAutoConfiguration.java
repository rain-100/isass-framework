// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.mq.spring.event;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;
import vip.isass.framework.mq.spring.event.consumer.SpringEventMqConsumerManager;
import vip.isass.framework.mq.spring.event.producer.SpringEventProducerManager;

@AutoConfiguration
@Import({
        SpringEventConfiguration.class,
        SpringEventMqFactory.class,
        SpringEventMqConsumerManager.class,
        SpringEventProducerManager.class
})
public class SpringEventAutoConfiguration {

}
