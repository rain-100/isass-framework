// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.mq.core;

/**
 * 消费业务抛出异常时的处理策略
 */
public enum FailStrategy {

    // 忽略错误，视为消费成功。消费管理器实现方应该向消息中间件响应消费成功的命令
    IGNORE,

    // 重试（默认策略）。重试的具体逻辑与策略，应由消息中间件响实现
    RETRY,

    // 立即重试，消费管理器实现方直接在本地重试消费，不经过消息中间件的干预。在某些不支持重试消费的消息中间件中，可用此策略实现重试功能
    RETRY_IMMEDIATELY;

}
