module vip.isass.framework.serialization.protobuf {
    // 依赖的标准Java平台模块
    requires java.base;

    // Google相关依赖
    requires com.google.common;
    requires com.google.protobuf;

    // 导出包
    exports vip.isass.framework.serialization.protobuf2;
} 