module vip.isass.framework.net.websocket {
    // 依赖的标准Java平台模块
    requires java.base;
    
    // 依赖的框架模块
    requires vip.isass.framework.common;
    requires vip.isass.framework.net.core;
    requires cn.hutool;
    requires io.netty.transport;
    requires io.netty.codec.http;
    requires io.netty.buffer;
    requires io.netty.codec;
    requires io.netty.common;
    requires vip.isass.framework.serialization.jackson;
    requires com.fasterxml.jackson.databind;
    requires com.google.auto.service;
    requires com.google.common;
    requires io.netty.handler;
    requires jakarta.annotation;
    requires org.slf4j;
    requires static lombok;

    // 导出包
    exports vip.isass.framework.net.websocket;
    exports vip.isass.framework.net.websocket.allocator;
    exports vip.isass.framework.net.websocket.packet;
    exports vip.isass.framework.net.websocket.session;
    exports vip.isass.framework.net.websocket.websocket;
} 