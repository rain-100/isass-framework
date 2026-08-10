// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.mq.kafka011.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @author Rain
 */
@Getter
@Setter
@ToString
@Accessors(chain = true)
@Configuration
@ConfigurationProperties("isass.mq.kafka011")
public class Kafka011Configuration {

    private boolean enable;

    private String defaultInstance;

    private List<InstanceConfiguration> instances;

}
