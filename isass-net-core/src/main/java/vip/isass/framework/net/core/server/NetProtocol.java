// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.core.server;

import lombok.Getter;

/**
 * 网络协议枚举
 */
public enum NetProtocol {

    tcp("tcp-service"),

    websocket("websocket-service"),

    socketio("socketio-service"),

    mqtt("mqtt-service");

    @Getter
    private final String serviceName;

    NetProtocol(String serviceName) {
        this.serviceName = serviceName;
    }

}
