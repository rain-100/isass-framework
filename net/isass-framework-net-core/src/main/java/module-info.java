module vip.isass.framework.net.core {
    // 依赖的框架模块
    requires vip.isass.framework.common;
    requires cn.hutool;
    requires jakarta.annotation;
    requires com.fasterxml.jackson.annotation;
    requires vip.isass.framework.serialization.jackson;
    requires vip.isass.framework.security.core;
    requires com.google.auto.service;
    requires org.slf4j;
    requires static lombok;

    // 导出包
    exports vip.isass.framework.net.core;
    exports vip.isass.framework.net.core.session;
    exports vip.isass.framework.net.core.server;
    exports vip.isass.framework.net.core.handler;
    exports vip.isass.framework.net.core.message;
    exports vip.isass.framework.net.core.handler.manager;
    exports vip.isass.framework.net.core.session.manage;
    exports vip.isass.framework.net.core.server.allocator;
} 