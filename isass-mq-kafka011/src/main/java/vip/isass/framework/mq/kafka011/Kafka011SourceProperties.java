package vip.isass.framework.mq.kafka011;

import lombok.Getter;
import lombok.Setter;
import vip.isass.framework.mq.core.config.MqSourceProperties;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class Kafka011SourceProperties extends MqSourceProperties {

    private String servers;

    private String producerId;

    private String consumerGroupId;

    private String defaultTopic;

    private String commonMessageTopic;

    private String shardingSequentialMessageTopic;

    private String globalSequentialMessageTopic;

    private String timingMessageTopic;

    private Map<String, String> properties = new HashMap<>();

    private Map<String, String> consumerProperties = new HashMap<>();
}
