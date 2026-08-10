// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.websocket.allocator;

import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import vip.isass.framework.net.core.server.NetProtocol;
import vip.isass.framework.net.core.server.NetServerInfo;
import vip.isass.framework.net.core.server.allocator.INodeAllocatorService;
import vip.isass.framework.net.websocket.WebsocketProperties;

import java.util.Collection;
import java.util.Collections;

@Configuration
@ConditionalOnProperty(name = "kernel.net.proxy.enabled", havingValue = "false", matchIfMissing = true)
public class WebsocketLocalNodeAllocatorService implements INodeAllocatorService, InitializingBean {

    private final WebsocketProperties websocketProperties;

    private final int httpPort;

    @Getter
    private final NetProtocol netProtocol = NetProtocol.websocket;

    private NetServerInfo netServerInfo;

    public WebsocketLocalNodeAllocatorService(WebsocketProperties websocketProperties,
                                               @Value("${server.port}") int httpPort) {
        this.websocketProperties = websocketProperties;
        this.httpPort = httpPort;
    }

    @Override
    public NetServerInfo allocate(String clientIp) {
        return netServerInfo;
    }

    @Override
    public Collection<NetServerInfo> getAll() {
        return Collections.singleton(netServerInfo);
    }

    @Override
    public void afterPropertiesSet() {
        String internalIp = NetUtil.getLocalhostStr();
        this.netServerInfo = NetServerInfo.builder()
                .netProtocol(netProtocol)
                .externalIp(StrUtil.blankToDefault(websocketProperties.getExternalIp(), internalIp))
                .internalIp(internalIp)
                .httpPort(httpPort)
                .httpSecure(Boolean.FALSE)
                .netExternalPort(websocketProperties.getNetExternalPort() == null
                        ? websocketProperties.getPort()
                        : websocketProperties.getNetExternalPort())
                .netExternalUrl(websocketProperties.getNetExternalUrl())
                .build();
        if (StrUtil.isBlank(netServerInfo.getNetExternalUrl())) {
            netServerInfo.setNetExternalUrl("ws://" + netServerInfo.getExternalIp() + ":" + netServerInfo.getNetExternalPort());
        }
    }
}
