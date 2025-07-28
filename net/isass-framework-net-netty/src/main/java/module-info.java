module vip.isass.framework.net.netty {
    // 依赖的框架模块
    requires vip.isass.framework.common;
    requires vip.isass.framework.net.core;
    requires jakarta.annotation;
    requires com.google.protobuf;
    requires io.netty.buffer;
    requires io.netty.codec;
    requires io.netty.transport;
    requires io.netty.codec.http;
    requires io.netty.common;
    requires vip.isass.framework.serialization.jackson;
    requires cn.hutool;
    requires com.fasterxml.jackson.databind;
    requires com.google.auto.service;
    requires io.netty.handler;
    requires com.google.common;
    requires org.slf4j;
    requires static lombok;

    // 导出包
    exports vip.isass.framework.net.netty;
} 