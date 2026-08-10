// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.core.handler.manager;

import vip.isass.framework.net.core.message.Message;
import vip.isass.framework.net.core.session.Session;

/**
 * 事件处理器
 *
 * @author Rain
 */
public interface IEventManager {

    /**
     * 连接创建事件
     *
     * @param session 会话
     */
    void onConnect(Session<?> session);

    /**
     * 断开连接事件
     *
     * @param session 会话
     */
    void onDisconnect(Session<?> session);

    /**
     * 收到路由消息事件
     *
     * @param message 消息
     */
    <T> void onMessage(Message message);

    /**
     * 收到错误消息事件
     *
     * @param session   会话
     * @param throwable 异常
     */
    void onError(Session<?> session, Throwable throwable);

}
