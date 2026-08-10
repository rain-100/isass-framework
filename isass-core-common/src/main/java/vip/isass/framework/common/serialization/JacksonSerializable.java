// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.serialization;

import tools.jackson.core.type.TypeReference;

/**
 * Jackson 可序列化接口
 *
 * @param <T> 序列化类型
 * @author Rain
 */
public interface JacksonSerializable<T> {

    TypeReference<T> typeReference();

}
