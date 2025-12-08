module vip.isass.framework.mq.kafka011 {
    // 依赖的框架模块
    requires vip.isass.framework.common;
    requires vip.isass.framework.mq.core;
    requires cn.hutool;
    requires jakarta.annotation;
    requires kafka.clients;
    requires vip.isass.framework.serialization.jackson;
    requires com.fasterxml.jackson.databind;
    requires org.slf4j;
    requires static lombok;
    requires com.google.auto.service;
    requires com.google.common;

    // 导出包
    exports vip.isass.framework.mq.kafka011;
    exports vip.isass.framework.mq.kafka011.config;
} 