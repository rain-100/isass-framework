module vip.isass.framework.database.redis {
    // 依赖的标准Java平台模块
    requires java.base;
    requires java.sql;
    
    // 依赖的框架模块
    requires vip.isass.framework.common;
    requires org.slf4j;
    requires spring.data.redis;
    requires vip.isass.framework.serialization.jackson;
    requires jakarta.annotation;
    requires cn.hutool;
    requires static lombok;
    requires spring.beans;
    requires spring.context;
    requires spring.boot.autoconfigure;

    // 导出包
    exports vip.isass.framework.database.redis;
} 