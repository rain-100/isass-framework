module vip.isass.framework.springboot.starter {
    // 依赖的框架模块
    requires vip.isass.framework.common;
    
    // 导出包
    exports vip.isass.framework.springboot.starter;
    exports vip.isass.framework.springboot.starter.database.init;
    
    // 提供异常映射服务
    provides vip.isass.framework.common.exception.IExceptionMapping 
        with vip.isass.framework.springboot.starter.SpringCoreExceptionMapping;
} 