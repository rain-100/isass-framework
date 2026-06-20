package vip.isass.framework.mq.spring.event.consumer;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import vip.isass.framework.mq.core.FailStrategy;
import vip.isass.framework.mq.core.MqMessage;
import vip.isass.framework.mq.core.consumer.IMqMessageHandler;
import vip.isass.framework.mq.spring.event.IsassMqEvent;

import java.util.Collections;
import java.util.List;

@Slf4j
public class SpringEventMqConsumer implements ApplicationListener<IsassMqEvent> {

    private final List<IMqMessageHandler> mqMessageHandlers;

    public SpringEventMqConsumer(List<IMqMessageHandler> mqMessageHandlers) {
        this.mqMessageHandlers = mqMessageHandlers == null
                ? Collections.emptyList()
                : List.copyOf(mqMessageHandlers);
    }

    @Override
    public void onApplicationEvent(IsassMqEvent event) {
        if (CollUtil.isEmpty(mqMessageHandlers)) {
            return;
        }
        MqMessage mqMessage = (MqMessage) event.getSource();
        mqMessageHandlers.stream()
                .filter(handler -> handler.getTopic().equals(mqMessage.getTopic()))
                .filter(handler -> "*".equals(handler.getTag()) || handler.getTag().equals(mqMessage.getTag()))
                .forEach(handler -> doConsume(handler, mqMessage));
    }

    private void doConsume(IMqMessageHandler mqMessageHandler, MqMessage mqMessage) {
        try {
            mqMessageHandler.consume(mqMessage);
        } catch (Exception e) {
            Throwable unwrap = ExceptionUtil.unwrap(e);
            log.error("mq consume failed: {}", unwrap.getMessage(), unwrap);
            FailStrategy failStrategy = mqMessageHandler.getFailStrategy();
            switch (failStrategy) {
                case IGNORE:
                    return;
                case RETRY:
                    retry(mqMessageHandler, mqMessage, 3);
                    return;
                case RETRY_IMMEDIATELY:
                    retry(mqMessageHandler, mqMessage, mqMessageHandler.getImmediatelyRetryCount());
                    return;
                default:
                    log.error("unsupported mq fail strategy [{}], consume is treated as complete", failStrategy);
            }
        }
    }

    private void retry(IMqMessageHandler mqMessageHandler, MqMessage mqMessage, int maxRetryCount) {
        for (int i = 0; i < maxRetryCount; i++) {
            try {
                mqMessageHandler.consume(mqMessage);
                return;
            } catch (Exception e) {
                log.error("mq retry consume failed: {}", e.getMessage(), e);
            }
        }
    }
}
