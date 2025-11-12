import vip.isass.framework.mq.core.consumer.IMqMessageHandler;
import vip.isass.framework.mq.core.producer.ProducerManager;

module vip.isass.framework.mq.core {
    uses ProducerManager;
    uses IMqMessageHandler;

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
    exports vip.isass.framework.mq.core.config;
} 