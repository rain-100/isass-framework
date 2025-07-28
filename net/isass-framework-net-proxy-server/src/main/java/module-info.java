module vip.isass.framework.net.proxy.server {
    // 依赖的框架模块
    requires vip.isass.framework.common;
    requires vip.isass.framework.net.core;
    requires cn.hutool;
    requires jakarta.annotation;
    requires static lombok;

    // 导出包
    exports vip.isass.framework.net.proxy.service;
} 