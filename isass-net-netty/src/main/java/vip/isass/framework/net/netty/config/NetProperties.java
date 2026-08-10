// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.netty.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author Rain
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "isass")
public class NetProperties {

    private int restTemplateTimeOut = 20_000;

}
