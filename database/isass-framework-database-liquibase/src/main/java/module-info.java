module vip.isass.framework.database.liquibase {
    // 依赖的框架模块
    requires vip.isass.framework.common;
    requires vip.isass.framework.database.core;
    
    // 依赖的标准Java平台模块
    requires java.sql;
    requires liquibase.core;

    // 导出包
} 