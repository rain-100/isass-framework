// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.mq;

/**
 * @author Rain
 */

public interface MessageType {

    /**
     * 普通消息，不保证消费顺序，性能最好
     */
    int COMMON_MESSAGE = 1;

    /**
     * 定时消息，Producer 将消息发送到 MQ 服务端，但并不期望这条消息立马投递，而是推迟到在当前时间点之后的某一个时间投递到 Consumer 进行消费，该消息即定时消息。
     */
    int TIMING_MESSAGE = 2;

    /**
     * 延时消息，Producer 将消息发送到 MQ 服务端，但并不期望这条消息立马投递，而是延迟一定时间后才投递到 Consumer 进行消费，该消息即延时消息。
     */
    int DELAY_MESSAGE = 3;

    /**
     * 事务消息，MQ 提供类似 X/Open XA 的分布事务功能，通过 MQ 事务消息能达到分布式事务的最终一致。
     */
    int TRANSACTION_MESSAGE = 4;

    /**
     * 分区顺序消息，对于指定的一个 Topic，所有消息根据 sharding key 进行区块分区。
     * 同一个分区内的消息按照严格的 FIFO 顺序进行发布和消费。
     * Sharding key 是顺序消息中用来区分不同分区的关键字段，和普通消息的 key 是完全不同的概念。
     * 示例
     * 【例一】用户注册需要发送发验证码，以用户 ID 作为 sharding key， 那么同一个用户发送的消息都会按照先后顺序来发布和订阅。
     * 【例二】电商的订单创建，以订单 ID 作为 sharding key，那么同一个订单相关的创建订单消息、订单支付消息、订单退款消息、订单物流消息都会按照先后顺序来发布和订阅。
     * 阿里巴巴集团内部电商系统均使用分区顺序消息，既保证业务的顺序，同时又能保证业务的高性能。
     */
    int SHARDING_SEQUENTIAL_MESSAGE = 5;

    /**
     * 全局顺序消息，对于指定的一个 Topic，所有消息按照严格的先入先出（FIFO）的顺序进行发布和消费。
     */
    int GLOBAL_SEQUENTIAL_MESSAGE = 6;

}
