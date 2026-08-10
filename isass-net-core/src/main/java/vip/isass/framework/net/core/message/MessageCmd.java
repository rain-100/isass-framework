// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.core.message;

/**
 * 内置消息路由
 *
 * @author Rain
 */
public interface MessageCmd {

    /**
     * core 的 cmd 前置
     */
    String CORE_PREFIX = "/core/";

    /**
     * PING
     */
    String PING = "/core/ping";

    /**
     * PONG
     */
    String PONG = "/core/pong";

    /**
     * 登录请求
     */
    String LOGIN = "/core/login";

    /**
     * 登出请求
     */
    String LOGOUT = "/core/logout";

    /**
     * 设置别名
     */
    String ALIAS = "/core/alias";

    /**
     * 异常报错
     */
    String ERROR = "/core/exception";

    /**
     * 客户端发起广播消息
     */
    String CLIENT_SEND_BROADCAST = "/core/clientSendBroadcast";

    /**
     * 客户端发起p2p消息
     */
    String CLIENT_P2P = "/core/client/p2p";

}
