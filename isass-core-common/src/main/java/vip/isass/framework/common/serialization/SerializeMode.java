// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.serialization;

import cn.hutool.core.util.StrUtil;

/**
 * 数据的序列化模式
 *
 * @author Rain
 */
public enum SerializeMode {

    /**
     * json模式
     */
    JSON(1),

    /**
     * protobuf2模式
     */
    PROTOBUF2(2),

    /**
     * protobuf3模式
     */
    PROTOBUF3(3);

    private Integer code;

    SerializeMode(Integer code) {
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }

    public static SerializeMode getByCode(Integer code) {
        for (SerializeMode serializationMode : SerializeMode.values()) {
            if (serializationMode.code.equals(code)) {
                return serializationMode;
            }
        }
        throw new IllegalArgumentException(StrUtil.format("id参数错误，不支持该参数值:{}", code));
    }
}
