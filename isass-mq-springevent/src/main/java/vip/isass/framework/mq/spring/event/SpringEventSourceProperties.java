// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.mq.spring.event;

import lombok.Getter;
import lombok.Setter;
import vip.isass.framework.mq.core.config.MqSourceProperties;

@Getter
@Setter
public class SpringEventSourceProperties extends MqSourceProperties {

    private String defaultTopic = "default";
}
