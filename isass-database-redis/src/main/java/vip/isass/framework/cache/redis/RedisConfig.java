// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.cache.redis;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.ReflectUtil;
import lombok.SneakyThrows;
import org.redisson.RedissonShutdownException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.hash.HashMapper;
import org.springframework.data.redis.hash.JacksonHashMapper;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import vip.isass.framework.common.support.JsonUtil;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author rain
 */
@Configuration(proxyBeanMethods = false)
@EnableCaching
public class RedisConfig extends CachingConfigurerSupport {

    Logger log = LoggerFactory.getLogger(RedisConfig.class);

    public static final HashMapper HASH_MAPPER = new JacksonHashMapper(JsonUtil.DEFAULT_INSTANCE, false);

    private static final RedisSerializer<String> KEY_SERIALIZER = RedisSerializer.string();

    private final RedisSerializer<Object> objectValueSerializer;

    private ThreadPoolTaskExecutor executor;

    public RedisConfig(BeanFactory beanFactory) {
        this.objectValueSerializer = createObjectValueSerializer(beanFactory);
    }

    private static RedisSerializer<Object> createObjectValueSerializer(BeanFactory beanFactory) {
        BasicPolymorphicTypeValidator.Builder validator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("vip.isass.")
                .allowIfSubType("java.lang.")
                .allowIfSubType("java.time.")
                .allowIfSubType("java.util.")
                .allowIfSubTypeIsArray();
        if (AutoConfigurationPackages.has(beanFactory)) {
            AutoConfigurationPackages.get(beanFactory).forEach(validator::allowIfSubType);
        }
        // 所有对象值统一保留运行时类型，并使用 Spring 的空值占位对象缓存 null。
        return GenericJacksonJsonRedisSerializer.builder()
                .customize(JsonUtil::configure)
                .enableDefaultTyping(validator.build())
                .enableSpringCacheNullValueSupport()
                .build();
    }

    private void initExecutor() {
        this.executor = new ThreadPoolTaskExecutor();
        this.executor.setCorePoolSize(10);
        this.executor.setMaxPoolSize(20);
        this.executor.setQueueCapacity(10000);
        this.executor.setKeepAliveSeconds(60);
        this.executor.setThreadNamePrefix("redis-subscriber-");
        this.executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        this.executor.initialize();
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig();

        // 配置序列化
        config.serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(KEY_SERIALIZER));
        config.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(objectValueSerializer));

        return RedisCacheManager.builder(redisConnectionFactory).cacheDefaults(config).build();
    }

    @Bean
    @SneakyThrows
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setDefaultSerializer(KEY_SERIALIZER);
        template.setKeySerializer(KEY_SERIALIZER);
        template.setHashKeySerializer(KEY_SERIALIZER);

        template.setValueSerializer(objectValueSerializer);
        template.setHashValueSerializer(objectValueSerializer);
        template.afterPropertiesSet();

        // 修改 stream 类型的 hashMapper
        Object o = ReflectUtil.newInstance(Class.forName("org.springframework.data.redis.core.DefaultStreamOperations"), template, HASH_MAPPER);
        ReflectUtil.setFieldValue(template, "streamOps", o);
        return template;
    }

    /**
     * 注册 redis pubsub 功能
     */
    @Bean
    public RedisMessageListenerContainer listenerContainer(RedisConnectionFactory connectionFactory,
                                                           @Autowired(required = false) List<IRedisSubscriber<?>> redisSubscribers) {
        initExecutor();
        RedisMessageListenerContainer listenerContainer = new RedisMessageListenerContainer();
        listenerContainer.setTaskExecutor(this.executor);
        listenerContainer.setConnectionFactory(connectionFactory);
        if (redisSubscribers == null) {
            return listenerContainer;
        }

        for (IRedisSubscriber<?> redisSubscriber : redisSubscribers) {
            listenerContainer.addMessageListener(redisSubscriber, redisSubscriber.topic());
        }
        return listenerContainer;
    }

    /**
     * 注册 redis stream 功能
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Bean(initMethod = "start", destroyMethod = "stop")
    public <T> StreamMessageListenerContainer streamMessageListenerContainer(RedisTemplate<String, ?> redisTemplate,
                                                                             RedisConnectionFactory connectionFactory,
                                                                             @Autowired(required = false) List<IRedisStreamListener<T>> listeners) {

        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, ObjectRecord<String, T>> options =
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions
                        .builder()
                        .objectMapper(RedisConfig.HASH_MAPPER)
                        .serializer(KEY_SERIALIZER)
                        .hashKeySerializer(KEY_SERIALIZER)
                        .keySerializer(KEY_SERIALIZER)
                        .hashValueSerializer(objectValueSerializer)
                        .pollTimeout(Duration.ofSeconds(5))
                        .batchSize(10)
                        .build();

        StreamMessageListenerContainer<String, ObjectRecord<String, T>> streamMessageListenerContainer =
                StreamMessageListenerContainer.create(connectionFactory, options);

        if (listeners != null) {
            for (IRedisStreamListener<T> listener : listeners) {
                Consumer consumer = Consumer.from(listener.getConsumerGroup(), listener.getConsumerName());

                try {
                    redisTemplate.opsForStream().createGroup(listener.getKey(), listener.getConsumerGroup());
                    log.info("redis stream[{}] consumer group[{}] created", listener.getKey(), listener.getConsumerGroup());
                } catch (Exception e) {
                    log.info("redis stream[{}] consumer group[{}] existing", listener.getKey(), listener.getConsumerGroup());
                }

                streamMessageListenerContainer.register(
                        StreamMessageListenerContainer.StreamReadRequest
                                .builder(StreamOffset.create(listener.getKey(), listener.getReadOffset()))
                                .consumer(consumer)
                                .autoAcknowledge(true)
                                .cancelOnError(throwable -> false)
                                .errorHandler(t -> {
                                    //noinspection unchecked
                                    if (ExceptionUtil.isCausedBy(t, RedissonShutdownException.class)) {
                                        log.error(ExceptionUtil.unwrap(t).getMessage());
                                        log.info("redis stream listener[{}|{}] is closed", listener.getKey(), consumer);
                                    } else {
                                        log.info("redis stream error[{}|{}] ", listener.getKey(), consumer, t);
                                    }
                                })
                                .build(),
                        listener::onMessage);
            }
        }

        return streamMessageListenerContainer;
    }

    @Bean
    public RedisStreamConsumerCleaner redisStreamConsumerCleaner(RedisTemplate<String, ?> redisTemplate,
                                                                  @Autowired(required = false) List<IRedisStreamListener<?>> listeners) {
        return new RedisStreamConsumerCleaner(redisTemplate, listeners);
    }

    @Bean
    public RedisStreamMessageCleaner redisStreamMessageCleaner(RedisTemplate<String, ?> redisTemplate,
                                                                @Autowired(required = false) List<IRemovableStreamMessageProvider> providers) {
        return new RedisStreamMessageCleaner(redisTemplate, providers);
    }

}
