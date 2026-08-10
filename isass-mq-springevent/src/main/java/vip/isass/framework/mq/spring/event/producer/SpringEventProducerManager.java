// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.mq.spring.event.producer;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import vip.isass.framework.mq.core.MqMessageContext;
import vip.isass.framework.mq.core.producer.ProducerManager;
import vip.isass.framework.mq.spring.event.SpringEventConfiguration;
import vip.isass.framework.mq.spring.event.SpringEventConst;

import jakarta.annotation.Resource;

/**
 * @author Rain
 */
@Component
public class SpringEventProducerManager implements ProducerManager {

    @Resource
    private ApplicationEventPublisher applicationEventPublisher;

    @Resource
    private SpringEventConfiguration springEventConfiguration;

    private SpringEventProducer springEventProducer;

    @Override
    public void init() {
        springEventProducer = new SpringEventProducer(applicationEventPublisher, springEventConfiguration);
    }

    @Override
    public String manufacturer() {
        return SpringEventConst.MANUFACTURER;
    }

    @Override
    public void destroy() {

    }

    @Override
    public boolean isEnable() {
        return springEventConfiguration.isEnable();
    }

    @Override
    public void send(MqMessageContext mqMessageContext) {
        springEventProducer.send(mqMessageContext);
    }

}
