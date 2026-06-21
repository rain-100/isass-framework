package vip.isass.framework.mq.redisstream;

import org.redisson.api.RedissonClient;
import vip.isass.framework.mq.core.IMqFactory;
import vip.isass.framework.mq.core.config.MqSourceProperties;
import vip.isass.framework.mq.core.consumer.IMqConsumerContainer;
import vip.isass.framework.mq.core.consumer.IMqMessageHandler;
import vip.isass.framework.mq.core.producer.IMqProducer;
import vip.isass.framework.mq.redisstream.consumer.RedisStreamMqConsumer;
import vip.isass.framework.mq.redisstream.producer.RedisStreamMqProducer;

import java.util.List;

public class RedisStreamMqFactory implements IMqFactory {

    private final RedissonClient redissonClient;

    public RedisStreamMqFactory(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public String getType() {
        return "redisstream";
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
        RedisStreamMqConsumer consumer = new RedisStreamMqConsumer(redissonClient, sourceProperties, mqMessageHandlers);
        consumer.start();
        return consumer::destroy;
    }

    @Override
    public IMqProducer createMqProducer(MqSourceProperties sourceProperties) {
        return new RedisStreamMqProducer(redissonClient);
    }
}
