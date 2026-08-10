// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.mq.core;

/**
 * @author Rain
 * 参考文档
 * https://help.aliyun.com/document_detail/43163.html?spm=a2c4g.11186623.2.5.18db510d11I6uF
 */
public enum SubscribeModel {

    /**
     * 广播消费模式
     * 当使用广播消费模式时，MQ 会将每条消息推送给集群内所有注册过的客户端，保证消息至少被每台机器消费一次。
     */
    BROADCASTING,

    /**
     * 集群消费模式
     * 当使用集群消费模式时，MQ 认为任意一条消息只需要被集群内的任意一个消费者处理即可。
     */
    CLUSTERING

}
