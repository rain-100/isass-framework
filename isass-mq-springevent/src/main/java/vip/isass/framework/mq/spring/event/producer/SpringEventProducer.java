// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.mq.spring.event.producer;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import vip.isass.framework.mq.core.MqMessageContext;
import vip.isass.framework.mq.core.producer.MqProducer;
import vip.isass.framework.mq.spring.event.IsassMqEvent;
import vip.isass.framework.mq.spring.event.SpringEventConfiguration;

/**
 * @author Rain
 */
@Slf4j
@Accessors(chain = true)
public class SpringEventProducer implements MqProducer {

    private final ApplicationEventPublisher applicationEventPublisher;

    private final SpringEventConfiguration springEventConfiguration;

    public SpringEventProducer(ApplicationEventPublisher applicationEventPublisher, SpringEventConfiguration springEventConfiguration) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.springEventConfiguration = springEventConfiguration;
    }

    @Override
    public void send(MqMessageContext mqMessageContext) {
        Assert.notNull(mqMessageContext);
        Assert.notNull(mqMessageContext.getTag(), "tag");
        Assert.notNull(mqMessageContext.getPayload(), "payload");
        if (StrUtil.isBlank(mqMessageContext.getTopic())) {
            mqMessageContext.setTopic(springEventConfiguration.getDefaultTopic());
        }

        try {
            applicationEventPublisher.publishEvent(new IsassMqEvent(mqMessageContext));
        } catch (Exception e) {
            log.error("mq发送失败。tag[{}], messageKey[{}]", mqMessageContext.getTag(), mqMessageContext.getKey());
            throw e;
        }
    }

    @Override
    public SpringEventProducer init() {
        return this;
    }

    @Override
    public void destroy() {

    }

}
