package vip.isass.framework.mq.spring.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.stereotype.Component;
import vip.isass.framework.mq.core.IMqFactory;
import vip.isass.framework.mq.core.config.MqSourceProperties;
import vip.isass.framework.mq.core.consumer.IMqConsumerContainer;
import vip.isass.framework.mq.core.consumer.IMqMessageHandler;
import vip.isass.framework.mq.core.producer.IMqProducer;
import vip.isass.framework.mq.spring.event.consumer.SpringEventMqConsumer;
import vip.isass.framework.mq.spring.event.producer.SpringEventMqProducer;

import java.util.List;

@Component
public class SpringEventMqFactory implements IMqFactory {

    private final ApplicationEventPublisher applicationEventPublisher;

    private final ApplicationEventMulticaster applicationEventMulticaster;

    public SpringEventMqFactory(ApplicationEventPublisher applicationEventPublisher,
                                ApplicationEventMulticaster applicationEventMulticaster) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.applicationEventMulticaster = applicationEventMulticaster;
    }

    @Override
    public String getType() {
        return "spring-event";
    }

    @Override
    public Class<? extends MqSourceProperties> getPropertiesType() {
        return SpringEventSourceProperties.class;
    }

    @Override
    public IMqConsumerContainer createMqConsumer(MqSourceProperties sourceProperties, List<IMqMessageHandler> mqMessageHandlers) {
        applicationEventMulticaster.addApplicationListener(new SpringEventMqConsumer(mqMessageHandlers));
        return null;
    }

    @Override
    public IMqProducer createMqProducer(MqSourceProperties sourceProperties) {
        String defaultTopic = "default";
        if (sourceProperties instanceof SpringEventSourceProperties springEventSourceProperties) {
            defaultTopic = springEventSourceProperties.getDefaultTopic();
        } else if (sourceProperties.getOptions() != null && sourceProperties.getOptions().containsKey("default-topic")) {
            defaultTopic = String.valueOf(sourceProperties.getOptions().get("default-topic"));
        }
        return new SpringEventMqProducer(applicationEventPublisher, defaultTopic);
    }
}
