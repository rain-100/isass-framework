// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.core.session;


import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.annotation.JsonValue;
import java.lang.reflect.ParameterizedType;
import vip.isass.framework.net.core.server.Server;

import java.lang.reflect.Type;

/**
 * 会话
 * <p> 一个客户端连接持有一个 session</p>
 * 不能在 session 对象，添加标签、别名等逻辑
 * 因为在 session 对象中保存标签信息，便无法实现方向查找，需要在上层的 sessionService 统一管理
 *
 * @param <svr> 服务端，代表 session 由哪个服务端创建
 * @author Rain
 */
public interface Session<svr extends Server> {

    /**
     * 判断连接是否可用
     *
     * @return 是否可用
     */
    boolean isActive();

    /**
     * 关闭通道连接，释放资源
     */
    void close();

    /**
     * 获取远端的 ip
     *
     * @return 远端的 ip
     */
    String getRemoteIp();

    /**
     * 获取远端的端口
     *
     * @return 远端的端口
     */
    String getRemotePort();

    /**
     * session id
     *
     * @return 会话唯一标识
     */
    String getSessionId();

    /**
     * 获取会话创建的时间
     *
     * @return 创建的时间
     */
    Long getCreateTime();

    /**
     * 发送消息
     *
     * @param cmd     路由命令
     * @param payload 消息体
     */
    void sendMessage(String cmd, Object payload);

    default Class<svr> getServerType() {
        Type genericInterface = this.getClass().getGenericInterfaces()[0];
        Type actualTypeArgument = ((ParameterizedType) genericInterface).getActualTypeArguments()[0];
        return ((Class<svr>) actualTypeArgument);
    }

    @JsonValue
    @JsonRawValue
    default String print() {
        return new StringBuilder()
                .append("{\"sessionId\":\"")
                .append(getSessionId())
                .append("\",\"remoteIp\":\"")
                .append(getRemoteIp())
                .append("\",\"remotePort\":\"")
                .append(getRemotePort())
                .append("\",\"createTime\":\"")
                .append(getCreateTime())
                .append("\",\"type\":\"")
                .append(this.getClass().getSimpleName())
                .append("\"}")
                .toString();
    }

}
