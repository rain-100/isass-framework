package vip.isass.framework.mq.redispubsub;

import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisPubSubMqAutoConfiguration {

    @Bean
    public RedisPubSubMqFactory redisPubSubMqFactory(RedissonClient redissonClient) {
        return new RedisPubSubMqFactory(redissonClient);
    }
}
