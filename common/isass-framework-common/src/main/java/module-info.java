module vip.isass.framework.common {
    // 依赖的标准Java平台模块
    requires java.base;
    requires java.sql;
    requires java.desktop;
    requires java.logging;

    requires cn.hutool;
    requires com.google.auto.service;
    requires com.google.common;
    requires org.slf4j;
    requires jakarta.activation;
    requires java.compiler;

    requires static lombok;

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
    exports vip.isass.framework.common.util.function;
    exports vip.isass.framework.common.security.authentication.login;
    exports vip.isass.framework.common.converter.datatime;
    exports vip.isass.framework.common.util.map;
    exports vip.isass.framework.common.security.authentication.jwt;
    exports vip.isass.framework.common.metadata;

    // 声明使用的服务接口
    uses vip.isass.framework.common.converter.Converter;
    uses vip.isass.framework.common.id.IdGenerator;
    uses vip.isass.framework.common.exception.IExceptionMapping;

    // 提供服务实现
    provides vip.isass.framework.common.id.IdGenerator
            with vip.isass.framework.common.id.impl.NoneIdGenerator,
                    vip.isass.framework.common.id.impl.UuidGenerator,
                    vip.isass.framework.common.id.impl.RandomLongIdGenerator,
                    vip.isass.framework.common.id.impl.RandomLongStringIdGenerator;
    provides vip.isass.framework.common.exception.IExceptionMapping
            with vip.isass.framework.common.exception.IsassCoreExceptionMapping;


} 