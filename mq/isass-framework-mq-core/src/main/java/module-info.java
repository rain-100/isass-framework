import vip.isass.framework.mq.core.consumer.MqConsumerManager;
import vip.isass.framework.mq.core.producer.ProducerManager;

module vip.isass.framework.mq.core {
    uses MqConsumerManager;
    uses ProducerManager;

    // 依赖的框架模块
    requires vip.isass.framework.common;
    requires jakarta.annotation;
    requires cn.hutool;
    requires org.slf4j;
    requires com.fasterxml.jackson.core;
    requires static lombok;

    // 导出包
    exports vip.isass.framework.mq.core;
    exports vip.isass.framework.mq.core.consumer;
    exports vip.isass.framework.mq.core.producer;
} 