module vip.isass.framework.database.core {
    // 依赖的标准Java平台模块
    requires java.base;
    requires java.sql;
    requires java.desktop;
    requires java.logging;

    // 依赖的框架模块
    requires vip.isass.framework.common;
    requires com.google.auto.service;
    requires org.javassist;
    requires org.slf4j;
    requires com.fasterxml.jackson.databind;
    requires static lombok;
    requires cn.hutool;

    // 导出数据库核心包
    exports vip.isass.framework.database.core;
    exports vip.isass.framework.database.core.exception;
    exports vip.isass.framework.database.core.init;
    exports vip.isass.framework.database.core.typehandler;
    exports vip.isass.framework.database.core.dameng;

    // 使用类型处理器服务
    uses vip.isass.framework.database.core.typehandler.IJsonNodeTypeHandler;
} 