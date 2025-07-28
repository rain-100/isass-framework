module vip.isass.framework.springboot.starter {
    // 依赖的框架模块
    requires vip.isass.framework.common;

    // Spring Framework相关依赖
    requires spring.beans;
    requires spring.context;
    requires spring.core;
    requires spring.aop;
    requires spring.boot;
    requires spring.boot.autoconfigure;

    // 其他依赖
    requires cn.hutool;
    requires org.slf4j;
    requires vip.isass.framework.database.core;
    requires jakarta.annotation;
    requires static lombok;

    // 导出包
    exports vip.isass.framework.springboot.starter;
    exports vip.isass.framework.springboot.starter.database.init;

    // 提供异常映射服务
    provides vip.isass.framework.common.exception.IExceptionMapping
            with vip.isass.framework.springboot.starter.SpringCoreExceptionMapping;
} 