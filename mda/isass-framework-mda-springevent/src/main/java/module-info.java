module vip.isass.framework.mda.spring.event {
    // 依赖的框架模块
    requires vip.isass.framework.common;
    requires vip.isass.framework.mda.core;
    requires spring.context;
    requires spring.beans;
    requires cn.hutool;
    requires jakarta.annotation;
    requires org.slf4j;
    requires static lombok;
    requires com.google.auto.service;

    // 导出包
    exports vip.isass.framework.mda.springevent;
    exports vip.isass.framework.mda.springevent.producer;
    exports vip.isass.framework.mda.springevent.consumer;
} 