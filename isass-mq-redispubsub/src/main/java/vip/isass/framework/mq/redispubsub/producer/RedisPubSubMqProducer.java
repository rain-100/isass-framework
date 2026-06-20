package vip.isass.framework.mq.redispubsub.producer;

import cn.hutool.core.util.StrUtil;
import org.redisson.api.RedissonClient;
import org.springframework.util.Assert;
import vip.isass.framework.mq.core.MqMessage;
import vip.isass.framework.mq.core.producer.IMqProducer;

public class RedisPubSubMqProducer implements IMqProducer {

    private final RedissonClient redissonClient;

    public RedisPubSubMqProducer(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public void init() {
    }

    @Override
    public void send(MqMessage mqMessage) {
        Assert.notNull(mqMessage, "mqMessage");
        Assert.isTrue(StrUtil.isNotBlank(mqMessage.getTopic()), "topic can not be blank");
        redissonClient.getTopic(mqMessage.getTopic()).publish(mqMessage);
    }

    @Override
    public void destroy() {
    }
}
