package vip.isass.framework.mq.spring.event.producer;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import vip.isass.framework.mq.core.MqMessage;
import vip.isass.framework.mq.core.producer.IMqProducer;
import vip.isass.framework.mq.spring.event.IsassMqEvent;

@Slf4j
public class SpringEventMqProducer implements IMqProducer {

    private final ApplicationEventPublisher applicationEventPublisher;

    private final String defaultTopic;

    public SpringEventMqProducer(ApplicationEventPublisher applicationEventPublisher, String defaultTopic) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.defaultTopic = defaultTopic;
    }

    @Override
    public void init() {
    }

    @Override
    public void destroy() {
    }

    @Override
    public void send(MqMessage mqMessage) {
        Assert.notNull(mqMessage, "mqMessage");
        Assert.notNull(mqMessage.getTag(), "tag");
        Assert.notNull(mqMessage.getPayload(), "payload");
        if (StrUtil.isBlank(mqMessage.getTopic())) {
            mqMessage.setTopic(defaultTopic);
        }
        try {
            applicationEventPublisher.publishEvent(new IsassMqEvent(mqMessage));
        } catch (Exception e) {
            log.error("mq send failed, topic[{}], tag[{}], key[{}]",
                    mqMessage.getTopic(), mqMessage.getTag(), mqMessage.getKey(), e);
            throw e;
        }
    }
}
