module vip.isass.framework.nocode.generator {
    // 依赖的框架模块
    requires vip.isass.framework.common;
    requires vip.isass.framework.nocode.core;

    requires cn.hutool;
    requires com.baomidou.mybatis.plus.annotation;
    requires com.baomidou.mybatis.plus.generator;
    requires freemarker;
    requires org.slf4j;
    requires static lombok;

    // 导出包
    exports vip.isass.framework.nocode.generator;
} 