module vip.isass.framework.security.springsecurity {
    // 依赖的框架模块
    requires vip.isass.framework.common;
    requires vip.isass.framework.security.core;
    requires spring.context;
    requires spring.beans;
    requires cn.hutool;
    requires jakarta.annotation;
    requires spring.data.redis;
    requires org.slf4j;
    requires static lombok;

    // 导出包
    exports vip.isass.framework.security.springsecurity;
} 