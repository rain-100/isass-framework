// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.core.server;

/**
 * 服务端
 *
 * @author Rain
 */
public interface Server {

    /**
     * 监听 hostname
     *
     * @return hostname
     */
    String getListeningAddress();

    /**
     * 启动服务端
     */
    void start();

    /**
     * 停止服务端
     */
    void stop();

    NetProtocol netProtocol();

}
