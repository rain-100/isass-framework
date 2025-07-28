module vip.isass.framework.rpc.okhttp {
    // 依赖的框架模块
    requires vip.isass.framework.common;
    requires cn.hutool;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.annotation;
    requires okhttp3;
    requires okhttp3.logging;
    requires org.slf4j;
    requires vip.isass.framework.serialization.jackson;
    requires static lombok;

    // 导出包
    exports vip.isass.framework.rpc.okhttp;
} 