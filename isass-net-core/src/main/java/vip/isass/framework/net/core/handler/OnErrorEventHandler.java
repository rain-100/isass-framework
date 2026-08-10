// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.core.handler;

import vip.isass.framework.net.core.session.Session;

/**
 * 收到错误消息事件处理器
 *
 * @author Rain
 */
public interface OnErrorEventHandler {

    /**
     * 收到错误消息事件
     *
     * @param session   会话
     * @param cmd       数据包
     * @param payload   数据图
     * @param throwable 异常
     */
    void onError(Session<?> session, String cmd, Object payload, Throwable throwable);

}
