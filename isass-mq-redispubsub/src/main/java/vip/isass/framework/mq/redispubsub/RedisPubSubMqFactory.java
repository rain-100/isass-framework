// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.mq.redispubsub;

import org.redisson.api.RedissonClient;
import vip.isass.framework.mq.core.IMqFactory;
import vip.isass.framework.mq.core.config.MqSourceProperties;
import vip.isass.framework.mq.core.consumer.IMqConsumerContainer;
import vip.isass.framework.mq.core.consumer.IMqMessageHandler;
import vip.isass.framework.mq.core.producer.IMqProducer;
import vip.isass.framework.mq.redispubsub.producer.RedisPubSubMqProducer;

import java.util.List;

public class RedisPubSubMqFactory implements IMqFactory {

    private final RedissonClient redissonClient;

    public RedisPubSubMqFactory(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public String getType() {
        return "redispubsub";
    }

    @Override
    public Class<? extends MqSourceProperties> getPropertiesType() {
        return MqSourceProperties.class;
    }

    @Override
    public IMqConsumerContainer createMqConsumer(MqSourceProperties sourceProperties, List<IMqMessageHandler> mqMessageHandlers) {
        if (mqMessageHandlers == null || mqMessageHandlers.isEmpty()) {
            return null;
        }
        mqMessageHandlers.forEach(handler -> redissonClient.getTopic(handler.getTopic())
                .addListener(vip.isass.framework.mq.core.MqMessage.class, (channel, mqMessage) -> {
                    if (!"*".equals(handler.getTag()) && !handler.getTag().equals(mqMessage.getTag())) {
                        return;
                    }
                    handler.consume(mqMessage);
                }));
        return null;
    }

    @Override
    public IMqProducer createMqProducer(MqSourceProperties sourceProperties) {
        return new RedisPubSubMqProducer(redissonClient);
    }
}
