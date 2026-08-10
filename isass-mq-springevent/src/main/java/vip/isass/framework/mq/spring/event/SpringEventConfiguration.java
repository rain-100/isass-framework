// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.mq.spring.event;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@ToString
@Accessors(chain = true)
@Configuration
@ConfigurationProperties("isass.mq.spring-event")
public class SpringEventConfiguration {

    private boolean enable;

    private String defaultTopic = "";

}
