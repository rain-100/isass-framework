// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.core.handler;

import vip.isass.framework.net.core.message.Message;
import vip.isass.framework.net.core.session.Session;

/**
 * 收到任何消息事件处理器
 *
 * @author Rain
 */
public interface OnAnyMessageEventHandler<T> {

    /**
     * 收到消息事件
     *
     * @param message 消息
     * @param payload 消息体
     * @return 需要响应的消息体
     */
    Object onMessage(Message message, T payload);

    /**
     * todo 获取分区键，同一分区键的事件会串行出来，不同分区键的事件会并行处理。
     * 默认返回空字符串，即事件默认会串行处理
     *
     * @param session 会话
     * @param cmd     路由命令
     * @param payload 消息体
     * @return 分区键
     */
    default String partitionKey(Session<?> session, String cmd, T payload) {
        return "";
    }

}
