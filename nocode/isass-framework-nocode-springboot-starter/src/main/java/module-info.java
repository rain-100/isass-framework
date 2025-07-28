module vip.isass.framework.nocode.springboot.starter {
    // 依赖的标准Java平台模块
    requires java.base;
    
    // 依赖的框架模块
    requires vip.isass.framework.common;
    requires vip.isass.framework.nocode.core;
    requires vip.isass.framework.nocode.generator;
    requires cn.hutool;
    requires org.apache.tomcat.embed.core;
    requires spring.context;
    requires vip.isass.framework.serialization.jackson;
    requires vip.isass.framework.web.springmvc;
    requires apm.toolkit.trace;
    requires spring.beans;
    requires spring.boot;
    requires org.slf4j;
    requires static lombok;

    // 导出包
    exports vip.isass.framework.nocode.springboot.starter;
} 