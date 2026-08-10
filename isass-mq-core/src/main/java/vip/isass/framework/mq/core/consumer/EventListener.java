// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.mq.core.consumer;

import vip.isass.framework.common.mq.MessageType;

import java.lang.annotation.*;


/**
 * @author Rain
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EventListener {

    /**
     * 提供实现的厂商
     * 例如：阿里云rocketmq、kafaka、rabbitmq
     *
     * @return manufacturer
     */
    String manufacturer() default "";

    /**
     * @return region
     */
    String region() default "";

    String instance() default "";

    String consumerId();

    int messageType() default MessageType.COMMON_MESSAGE;

    /**
     * 消息主题，一级消息类型，通过 Topic 对消息进行分类。
     *
     * @return topic
     */
    String topic() default "";

    /**
     * 消息标签，二级消息类型，用来进一步区分某个 Topic 下的消息分类。
     *
     * @return tag
     */
    String tag() default "*";

    /**
     * 设置 Consumer 实例的消费线程数，默认值
     *
     * @return the consumer thread number
     */
    int consumeThreadNumber() default -1;

    /**
     * 消息的业务标识，由消息生产者（Producer）设置，唯一标识某个业务逻辑。(一般与tag同值)
     *
     * @return key
     */
    String key() default "";

}
