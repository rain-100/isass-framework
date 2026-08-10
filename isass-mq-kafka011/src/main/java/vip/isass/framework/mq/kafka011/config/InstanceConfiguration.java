// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.mq.kafka011.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;

/**
 * kafka011 mq实例配置
 *
 * @author Rain
 */
@Getter
@Setter
@ToString
@Accessors(chain = true)
public class InstanceConfiguration {

    private String instanceName;

    private String servers;

    private String enableAutoCommit;

    private String autoCommitIntervalMs;

    private String autoOffsetReset;

    private String sessionTimeoutMs;

    private String defaultTopic;

    private String commonMessageTopic;

    private String shardingSequentialMessageTopic;

    private String globalSequentialMessageTopic;

    private String timingMessageTopic;

    private String defaultProducer;

    private Map<String, String> properties;

    private List<ProducerConfiguration> producers;

}
