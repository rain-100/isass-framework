import vip.isass.framework.mq.core.consumer.IMdaMessageHandler;

module vip.isass.framework.mq.core {
    uses IMdaMessageHandler;

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
    exports vip.isass.framework.mq.core.message;
} 