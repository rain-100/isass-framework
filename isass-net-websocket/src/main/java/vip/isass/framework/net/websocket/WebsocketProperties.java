// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.websocket;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "kernel.net.websocket")
public class WebsocketProperties {

    private String hostName = "0.0.0.0";

    private int port = 20071;

    /**
     * 链路空闲超时时间(ms)，包括读和写
     */
    private int timeout = 300_000;

    private String externalIp;

    private Integer netExternalPort;

    private String netExternalUrl;

    private int aggregator = 65536;

    private int maxFramePayloadLength = 10 * 1024 * 1024;

}
