import vip.isass.framework.mda.core.consumer.IMdaMessageHandler;

module vip.isass.framework.mda.core {
    uses IMdaMessageHandler;

    // 依赖的框架模块
    requires vip.isass.framework.common;
    requires jakarta.annotation;
    requires cn.hutool;
    requires org.slf4j;
    requires com.fasterxml.jackson.core;
    requires static lombok;

    // 导出包
    exports vip.isass.framework.mda.core;
    exports vip.isass.framework.mda.core.consumer;
    exports vip.isass.framework.mda.core.producer;
    exports vip.isass.framework.mda.core.config;
    exports vip.isass.framework.mda.core.message;
} 