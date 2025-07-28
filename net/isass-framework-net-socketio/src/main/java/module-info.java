module vip.isass.framework.net.socketio {
    // 依赖的框架模块
    requires vip.isass.framework.common;
    requires vip.isass.framework.net.core;
    requires cn.hutool;
    requires netty.socketio;
    requires com.google.auto.service;
    requires jakarta.annotation;
    requires io.netty.transport;
    requires org.slf4j;
    requires static lombok;

    // 导出包
    exports vip.isass.framework.net.socketio;
} 