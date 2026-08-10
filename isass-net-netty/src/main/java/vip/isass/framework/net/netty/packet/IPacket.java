// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.netty.packet;

import vip.isass.framework.common.serialization.SerializeMode;

/**
 * 完整的数据包，此类应该在 net 模块内部使用，不提供给上层应用开发者
 *
 * @author Rain
 */
public interface IPacket {

    /**
     * body 的序列化方式
     *
     * @return serialize mode
     * @see SerializeMode
     */
    Integer getSerializeMode();

    /**
     * 设置序列化模式
     *
     * @param serializeMode 序列化模式
     */
    void setSerializeMode(Integer serializeMode);

    /**
     * 获取路由命令
     *
     * @return 路由命令
     */
    String getCmd();

    /**
     * 设置路由命令
     *
     * @param cmd 路由命令
     */
    void setCmd(String cmd);

    /**
     * 获取消息体
     *
     * @return 消息体
     */
    Object getPayload();

    /**
     * 设置消息体
     *
     * @param content 消息体
     */
    void setPayload(Object content);

}
