// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.core.client;

/**
 * 客户端
 *
 * @author Rain
 */
public interface Client {

    /**
     * 发起连接
     */
    void connect();

    /**
     * 断开连接
     */
    void disconnect();

    /**
     * 重新连接
     */
    void reconnect();

}
