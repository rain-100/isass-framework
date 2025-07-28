module vip.isass.framework.database.mybatisplus {
    // 依赖的标准Java平台模块
    requires java.base;
    requires java.sql;

    // 依赖的框架模块
    requires vip.isass.framework.common;
    requires vip.isass.framework.database.core;
    requires com.fasterxml.jackson.databind;
    requires com.baomidou.mybatis.plus.core;
    requires org.mybatis;
    requires org.slf4j;
    requires cn.hutool;
    requires com.baomidou.mybatis.plus.extension;
    requires vip.isass.framework.serialization.jackson;
    requires org.postgresql.jdbc;
    requires spring.tx;
    requires spring.jdbc;
    requires spring.context;
    requires static lombok;
    requires org.yaml.snakeyaml;

    // 导出包
    exports vip.isass.framework.database.orm.mybatisplus.jackson;
    exports vip.isass.framework.database.orm.mybatisplus.typehandler;
    exports vip.isass.framework.database.orm.mybatisplus.typehandler.json;
    exports vip.isass.framework.database.orm.mybatisplus.exception;

    // 提供服务实现
    provides vip.isass.framework.common.exception.IExceptionMapping
            with vip.isass.framework.database.orm.mybatisplus.exception.BuildInDatabaseExceptionMapping;
    provides vip.isass.framework.database.core.typehandler.IJsonNodeTypeHandler
            with vip.isass.framework.database.orm.mybatisplus.typehandler.MysqlJsonNodeTypeHandler,
                    vip.isass.framework.database.orm.mybatisplus.typehandler.PostgresqlJsonNodeTypeHandler;
} 