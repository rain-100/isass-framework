module vip.isass.framework.web.springmvc {
    // 依赖的框架模块
    requires vip.isass.framework.common;
    requires vip.isass.framework.serialization.jackson;
    
    // Spring Framework相关依赖
    requires spring.beans;
    requires spring.context;
    requires spring.core;
    requires spring.web;
    requires spring.webmvc;
    requires spring.boot;
    requires spring.boot.autoconfigure;
    
    // HTTP和Web相关依赖
    requires java.desktop;
    requires org.apache.tomcat.embed.core;
    
    // Jackson序列化依赖
    requires com.fasterxml.jackson.databind;
    
    // Hutool工具类
    requires cn.hutool;
    
    // Lombok
    requires static lombok;
    
    // SLF4J日志
    requires org.slf4j;
    
    // 导出包
    exports vip.isass.framework.web.springmvc;
    exports vip.isass.framework.web.springmvc.config;
    exports vip.isass.framework.web.springmvc.execption;
    exports vip.isass.framework.web.springmvc.interceptor;
} 