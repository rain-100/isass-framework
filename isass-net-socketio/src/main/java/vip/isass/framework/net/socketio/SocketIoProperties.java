// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.socketio;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "kernel.net.socketio")
public class SocketIoProperties {

    private boolean enabled;

    private String hostName = "0.0.0.0";

    private int port = 20001;

    private int maxHttpContentLength = 64 * 1024;

    private int maxFramePayloadLength = 64 * 1024;

    private String externalIp;

    private Integer netExternalPort;

    private String netExternalUrl;

    private String keyStorePath;

    private String keyStoreFormat;

    private String keyStorePassword;
}
