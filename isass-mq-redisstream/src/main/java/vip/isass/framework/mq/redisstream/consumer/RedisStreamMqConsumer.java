// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.mq.redisstream.consumer;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.stream.StreamCreateGroupArgs;
import org.redisson.api.stream.StreamMessageId;
import org.redisson.api.stream.StreamReadGroupArgs;
import vip.isass.framework.mq.core.FailStrategy;
import vip.isass.framework.mq.core.MqMessage;
import vip.isass.framework.mq.core.config.MqSourceProperties;
import vip.isass.framework.mq.core.consumer.IMqMessageHandler;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
public class RedisStreamMqConsumer {

    private final RedissonClient redissonClient;

    private final MqSourceProperties sourceProperties;

    private final List<IMqMessageHandler> mqMessageHandlers;

    private final AtomicBoolean running = new AtomicBoolean();

    private ExecutorService executorService;

    public RedisStreamMqConsumer(RedissonClient redissonClient, MqSourceProperties sourceProperties,
                                 List<IMqMessageHandler> mqMessageHandlers) {
        this.redissonClient = redissonClient;
        this.sourceProperties = sourceProperties;
        this.mqMessageHandlers = List.copyOf(mqMessageHandlers);
    }

    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        Map<String, List<IMqMessageHandler>> handlersByTopic = mqMessageHandlers.stream()
                .collect(Collectors.groupingBy(IMqMessageHandler::getTopic));
        executorService = Executors.newFixedThreadPool(handlersByTopic.size(), runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("isass-mq-redisstream-" + sourceProperties.getName());
            thread.setDaemon(true);
            return thread;
        });
        handlersByTopic.forEach((topic, handlers) -> executorService.execute(() -> poll(topic, handlers)));
    }

    public void destroy() {
        running.set(false);
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    private void poll(String topic, List<IMqMessageHandler> handlers) {
        RStream<String, Object> stream = redissonClient.getStream(topic);
        String consumerGroup = option("consumer-group", StrUtil.blankToDefault(sourceProperties.getName(), "default"));
        String consumerName = option("consumer-name", sourceProperties.getName() + "-" + Integer.toHexString(hashCode()));
        createGroup(stream, consumerGroup);
        while (running.get()) {
            try {
                Map<StreamMessageId, Map<String, Object>> messages = stream.readGroup(
                        consumerGroup,
                        consumerName,
                        StreamReadGroupArgs.neverDelivered()
                                .count(optionInt("batch-size", 10))
                                .timeout(Duration.ofMillis(optionLong("poll-timeout-millis", 1000L))));
                if (messages == null || messages.isEmpty()) {
                    continue;
                }
                messages.forEach((messageId, body) -> {
                    MqMessage mqMessage = toMqMessage(body);
                    if (mqMessage == null) {
                        stream.ack(consumerGroup, messageId);
                        return;
                    }
                    handlers.stream()
                            .filter(handler -> "*".equals(handler.getTag()) || Objects.equals(handler.getTag(), mqMessage.getTag()))
                            .forEach(handler -> consume(handler, mqMessage));
                    stream.ack(consumerGroup, messageId);
                });
            } catch (Exception e) {
                if (running.get()) {
                    log.error("redis stream mq consume failed: {}", ExceptionUtil.unwrap(e).getMessage(), e);
                }
            }
        }
    }

    private void createGroup(RStream<String, Object> stream, String consumerGroup) {
        try {
            stream.createGroup(StreamCreateGroupArgs.name(consumerGroup).makeStream().id(StreamMessageId.LAST));
        } catch (Exception e) {
            log.debug("redis stream group [{}] already exists or can not be created: {}", consumerGroup, e.getMessage());
        }
    }

    private MqMessage toMqMessage(Map<String, Object> body) {
        Object value = body.get("message");
        return value instanceof MqMessage mqMessage ? mqMessage : null;
    }

    private void consume(IMqMessageHandler handler, MqMessage mqMessage) {
        try {
            handler.consume(mqMessage);
        } catch (Exception e) {
            log.error("redis stream mq handler consume failed: {}", ExceptionUtil.unwrap(e).getMessage(), e);
            if (handler.getFailStrategy() == FailStrategy.RETRY_IMMEDIATELY) {
                for (int i = 0; i < handler.getImmediatelyRetryCount(); i++) {
                    try {
                        handler.consume(mqMessage);
                        return;
                    } catch (Exception retryError) {
                        log.error("redis stream mq retry consume failed: {}", retryError.getMessage(), retryError);
                    }
                }
            }
        }
    }

    private String option(String key, String defaultValue) {
        Object value = sourceProperties.getOptions().get(key);
        return value == null ? defaultValue : String.valueOf(value);
    }

    private int optionInt(String key, int defaultValue) {
        Object value = sourceProperties.getOptions().get(key);
        return value == null ? defaultValue : Integer.parseInt(String.valueOf(value));
    }

    private long optionLong(String key, long defaultValue) {
        Object value = sourceProperties.getOptions().get(key);
        return value == null ? defaultValue : Long.parseLong(String.valueOf(value));
    }
}
