module vip.isass.framework.security.core {
    // 依赖的标准Java平台模块
    requires java.base;

    // 依赖的框架模块
    requires vip.isass.framework.common;
    requires cn.hutool;
    requires jjwt.api;
    requires static lombok;

    // 导出包
    exports vip.isass.framework.security.core;
    exports vip.isass.framework.security.core.authentication.jwt;
    exports vip.isass.framework.security.core.authentication.login;
    exports vip.isass.framework.security.core.authentication.multiterminal;
    exports vip.isass.framework.security.core.authorization.permiturl;
    exports vip.isass.framework.security.core.authorization.role;
    exports vip.isass.framework.security.core.crypto.rsa;
    exports vip.isass.framework.security.core.exception;
} 