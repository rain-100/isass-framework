// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.mq.core;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import vip.isass.framework.common.mq.MessageType;
import vip.isass.framework.common.support.SystemClock;

import java.util.Map;

/**
 * @author Rain
 */
@Getter
@Setter
@Accessors(chain = true)
public class MqMessage implements MqMessageContext {

    private String manufacturer;

    private int messageType = MessageType.COMMON_MESSAGE;

    private String topic;

    private String tag;

    private String key;

    private String shardingKey;

    private Long consumeAtMills;

    private Long delayMills;

    private Object payload;

    private Map<String, Object> properties;

    private long createTime = SystemClock.now();

    @Override
    public String toString() {
        return "MqMessage{" +
            "manufacturer='" + manufacturer + '\'' +
            ", messageType=" + messageType +
            ", topic='" + topic + '\'' +
            ", tag='" + tag + '\'' +
            ", key='" + key + '\'' +
            ", shardingKey='" + shardingKey + '\'' +
            ", consumeAtMills=" + consumeAtMills +
            ", delayMills=" + delayMills +
            ", payload=" + payload +
            ", properties=" + properties +
            ", createTime=" + createTime +
            '}';
    }

}
