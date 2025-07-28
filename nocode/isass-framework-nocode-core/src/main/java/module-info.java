module vip.isass.framework.nocode.core {
    exports vip.isass.framework.nocode.v2.entity;
    // 依赖的框架模块
    requires vip.isass.framework.common;
    requires cn.hutool;
    requires java.desktop;
    requires org.slf4j;
    requires vip.isass.framework.serialization.jackson;
    requires com.fasterxml.jackson.databind;
    requires static lombok;

    // 导出包
}