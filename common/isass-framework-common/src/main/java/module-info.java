module vip.isass.framework.common {
    // 依赖的标准Java平台模块
    requires java.base;
    requires java.sql;
    requires java.desktop;
    requires java.logging;
    
    // 导出公共包供其他模块使用
    exports vip.isass.framework.common;
    exports vip.isass.framework.common.converter;
    exports vip.isass.framework.common.entrypoint;
    exports vip.isass.framework.common.exception;
    exports vip.isass.framework.common.exception.code;
    exports vip.isass.framework.common.id;
    exports vip.isass.framework.common.id.impl;
    exports vip.isass.framework.common.log;
    exports vip.isass.framework.common.page;
    exports vip.isass.framework.common.serialization;
    exports vip.isass.framework.common.service;
    exports vip.isass.framework.common.support;
    exports vip.isass.framework.common.util;
    
    // 声明使用的服务接口
    uses vip.isass.framework.common.converter.Converter;
    uses vip.isass.framework.common.id.IdGenerator;
    uses vip.isass.framework.common.exception.IExceptionMapping;
    
    // 提供服务实现
    provides vip.isass.framework.common.id.IdGenerator 
        with vip.isass.framework.common.id.impl.UuidGenerator;
    provides vip.isass.framework.common.exception.IExceptionMapping 
        with vip.isass.framework.common.exception.IsassCoreExceptionMapping;
} 