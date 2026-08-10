// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.mq.kafka011.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.Map;

/**
 * 阿里云mq实例配置
 *
 * @author Rain
 */
@Getter
@Setter
@ToString
@Accessors(chain = true)
public class ProducerConfiguration {

    private String producerId;

    private String defaultTopic;

    private Map<String, String> properties;

}
