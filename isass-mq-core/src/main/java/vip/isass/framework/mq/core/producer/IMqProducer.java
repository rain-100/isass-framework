package vip.isass.framework.mq.core.producer;

import vip.isass.framework.mq.core.MqMessage;

public interface IMqProducer {

    void init();

    void destroy();

    void send(MqMessage mqMessage);
}
