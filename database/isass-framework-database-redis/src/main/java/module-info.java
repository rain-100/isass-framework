module vip.isass.framework.database.redis {
    // 依赖的标准Java平台模块
    requires java.base;
    
    // 依赖的框架模块
    requires vip.isass.framework.common;
    requires vip.isass.framework.database.core;
    
    // 导出包
    exports vip.isass.framework.database.redis;
} 