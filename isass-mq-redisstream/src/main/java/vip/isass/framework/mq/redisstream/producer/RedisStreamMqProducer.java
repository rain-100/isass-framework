package vip.isass.framework.mq.redisstream.producer;

import cn.hutool.core.util.StrUtil;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.stream.StreamAddArgs;
import org.springframework.util.Assert;
import vip.isass.framework.mq.core.MqMessage;
import vip.isass.framework.mq.core.producer.IMqProducer;

public class RedisStreamMqProducer implements IMqProducer {

    private final RedissonClient redissonClient;

    public RedisStreamMqProducer(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public void init() {
    }

    @Override
    public void send(MqMessage mqMessage) {
        Assert.notNull(mqMessage, "mqMessage");
        Assert.isTrue(StrUtil.isNotBlank(mqMessage.getTopic()), "topic can not be blank");
        RStream<String, Object> stream = redissonClient.getStream(mqMessage.getTopic());
        stream.add(StreamAddArgs.entry("message", mqMessage));
    }

    @Override
    public void destroy() {
    }
}
