// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.core.message;

/**
 * 消息发送器类型
 *
 * @author rain
 */
public enum SenderType {

    /**
     * 直接模式。此模式的发送器，持有长链接通道，直接通过此通道发送消息
     */
    DIRECT,

    /**
     * 中转模式。此模式的发送器，会将消息发送到中转站，例如 redis
     */
    TRANSFER;

}
