// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.mq.redisstream;

import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisStreamMqAutoConfiguration {

    @Bean
    public RedisStreamMqFactory redisStreamMqFactory(RedissonClient redissonClient) {
        return new RedisStreamMqFactory(redissonClient);
    }
}
