// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.proxy.core;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

@AutoConfiguration
@EnableScheduling
@ConditionalOnProperty(name = {"kernel.net.enabled", "kernel.net.proxy.enabled"}, havingValue = "true")
public class NetProxyCoreAutoConfiguration {

    @Bean
    public CmdRedisService cmdRedisService(RedisTemplate<String, Object> redisTemplate) {
        return new CmdRedisService(redisTemplate);
    }

}
