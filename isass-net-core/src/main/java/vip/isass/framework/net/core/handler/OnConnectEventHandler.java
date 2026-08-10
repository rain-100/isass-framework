// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.core.handler;

import vip.isass.framework.net.core.session.Session;

/**
 * 连接创建事件处理器
 *
 * @author Rain
 */
public interface OnConnectEventHandler {

    /**
     * 连接创建事件
     *
     * @param session 会话
     */
    void onConnect(Session<?> session);

}
