// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.core.handler;

import vip.isass.framework.net.core.session.Session;

/**
 * 断开连接事件处理器
 *
 * @author Rain
 */
public interface OnDisconnectEventHandler {

    /**
     * 断开连接事件
     *
     * @param session 会话
     */
    void onDisconnect(Session<?> session);

}
