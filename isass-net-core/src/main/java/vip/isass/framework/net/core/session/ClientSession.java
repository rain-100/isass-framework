// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.core.session;

import vip.isass.framework.net.core.server.Server;

/**
 * 客户端会话，服务端接收客户端连接时，在服务端持有此会话
 *
 * @param <svr> 服务端的具体实现类
 * @author Rain
 */
public interface ClientSession<svr extends Server> extends Session<svr> {

}
