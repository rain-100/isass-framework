// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.core.handler;

import java.util.Collection;

/**
 * 消息事件处理器的注册器，长连接实现方实现此接口，把消息事件处理器绑定到长连接实现服务
 *
 * @author rain
 */
public interface IMessageEventRegister {

    /**
     * 监听路由命令
     *
     * @param commands 路由命令
     */
    void listening(Collection<String> commands);

    /**
     * 删除监听
     * 取消监听路由命令
     *
     * @param commands 路由命令
     */
    void removeListening(Collection<String> commands);

}
