// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.proxy.upstream;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.RandomUtil;
import lombok.extern.slf4j.Slf4j;
import org.redisson.RedissonShutdownException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import vip.isass.framework.cache.redis.RedisConfig;
import vip.isass.framework.net.core.NetRedisKey;
import vip.isass.framework.net.core.handler.manager.IEventManager;
import vip.isass.framework.net.core.message.Message;

import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author rain
 */
@Slf4j
public class MessageRedisStreamListener implements StreamListener<String, ObjectRecord<String, Message>> {

    private final IEventManager eventManager;

    private final RedisTemplate<String, Object> redisTemplate;

    private final String streamServiceKey;

    private ThreadPoolTaskExecutor executor;

    private static final Consumer CONSUMER = Consumer.from(NetRedisKey.CONSUMER_GROUP, RandomUtil.randomString(6));

    public MessageRedisStreamListener(IEventManager eventManager,
                                      RedisTemplate<String, Object> redisTemplate,
                                      @Value("${spring.application.name:}") String applicationName) {
        this.eventManager = eventManager;
        this.redisTemplate = redisTemplate;
        this.streamServiceKey = NetRedisKey.REDIS_STREAM_PREFIX_KEY + applicationName;
    }

    private void initExecutor() {
        this.executor = new ThreadPoolTaskExecutor();
        this.executor.setCorePoolSize(10);
        this.executor.setMaxPoolSize(20);
        this.executor.setQueueCapacity(10000);
        this.executor.setKeepAliveSeconds(60);
        this.executor.setThreadNamePrefix("redis-stream-");
        this.executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        this.executor.initialize();
    }

    @Override
    public void onMessage(ObjectRecord<String, Message> message) {
        Message value = message.getValue();
        eventManager.onMessage(value);
    }

    @PreDestroy
    public void destroy() {
        try {
            log.info("正在清理 redis stream[{}]的消费者[{}]", streamServiceKey, CONSUMER);

            redisTemplate.opsForStream().deleteConsumer(streamServiceKey, CONSUMER);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 监听本微服务和 unknowns 的中转消息
     *
     * @param factory redis 连接工厂
     * @return StreamMessageListenerContainer
     */
    public StreamMessageListenerContainer<String, ObjectRecord<String, Message>>
    netTransferMessageListenerContainer(RedisConnectionFactory factory) {
        initExecutor();

        @SuppressWarnings("unchecked")
        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, ObjectRecord<String, Message>> options =
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions
                        .builder()
                        .objectMapper(RedisConfig.HASH_MAPPER)
                        .serializer(redisTemplate.getDefaultSerializer())
                        .hashValueSerializer(redisTemplate.getHashValueSerializer())
                        .pollTimeout(Duration.ofSeconds(4))
                        .batchSize(5)
                        .targetType(Message.class)
                        .executor(this.executor)
                        .build();

        StreamMessageListenerContainer<String, ObjectRecord<String, Message>> listenerContainer =
                StreamMessageListenerContainer.create(factory, options);

        // 创建本微服务的消费组
        try {
            redisTemplate.opsForStream().createGroup(streamServiceKey, CONSUMER.getGroup());
            log.info("redis stream[{}]的消费者组[{}]已创建", streamServiceKey, CONSUMER.getGroup());
        } catch (Exception e) {
            log.info("redis stream[{}]的消费者组[{}]已存在", streamServiceKey, CONSUMER.getGroup());
        }

        // 创建 unknowns 的消费组
        try {
            redisTemplate.opsForStream().createGroup(NetRedisKey.REDIS_STREAM_UNKNOWN_SERVICE_KEY, CONSUMER.getGroup());
            log.info("redis stream[{}]的消费者组[{}]已创建", NetRedisKey.REDIS_STREAM_UNKNOWN_SERVICE_KEY, CONSUMER.getGroup());
        } catch (Exception e) {
            log.info("redis stream[{}]的消费者组[{}]已存在", NetRedisKey.REDIS_STREAM_UNKNOWN_SERVICE_KEY, CONSUMER.getGroup());
        }

        // 监听本微服务的中转消息
        listenerContainer.register(
                StreamMessageListenerContainer.StreamReadRequest
                        .builder(StreamOffset.create(streamServiceKey, ReadOffset.lastConsumed()))
                        .consumer(CONSUMER)
                        .autoAcknowledge(true)
                        .cancelOnError(throwable -> false)
                        .errorHandler(t -> {
                            //noinspection unchecked
                            if (ExceptionUtil.isCausedBy(t, RedissonShutdownException.class)) {
                                log.info("redis stream listener[{}:{}]is closed", streamServiceKey, CONSUMER);
                            } else {
                                throw new RuntimeException(t);
                            }
                        })
                        .build(),
                this);

        // 监听 unknowns 的中转消息
        listenerContainer.register(
                StreamMessageListenerContainer.StreamReadRequest
                        .builder(StreamOffset.create(NetRedisKey.REDIS_STREAM_UNKNOWN_SERVICE_KEY, ReadOffset.lastConsumed()))
                        .consumer(CONSUMER)
                        .autoAcknowledge(true)
                        .cancelOnError(throwable -> false)
                        .errorHandler(t -> {
                            //noinspection unchecked
                            if (ExceptionUtil.isCausedBy(t, RedissonShutdownException.class)) {
                                log.info("redis stream listener[{}:{}]is closed", NetRedisKey.REDIS_STREAM_UNKNOWN_SERVICE_KEY, CONSUMER);
                            } else {
                                throw new RuntimeException(t);
                            }
                        })
                        .build(),
                this);
        return listenerContainer;
    }

}
