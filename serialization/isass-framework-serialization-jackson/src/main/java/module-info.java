module vip.isass.framework.serialization.jackson {
    // 依赖的框架模块
    requires vip.isass.framework.common;
    requires cn.hutool;
    requires com.fasterxml.jackson.databind;
    requires org.slf4j;
    requires static lombok;
    requires com.google.auto.service;

    // 导出包
    exports vip.isass.framework.serialization.jackson;
    exports vip.isass.framework.serialization.jackson.converter;
    exports vip.isass.framework.serialization.jackson.converter.stdconverter;
    exports vip.isass.framework.serialization.jackson.serializer;
} 