// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.socketio.allocator;

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
import vip.isass.framework.net.socketio.SocketIoProperties;

import java.util.Collection;
import java.util.Collections;

@Configuration
@ConditionalOnProperty(name = "kernel.net.proxy.enabled", havingValue = "false", matchIfMissing = true)
public class SocketIoLocalNodeAllocatorService implements INodeAllocatorService, InitializingBean {

    private final SocketIoProperties socketIoProperties;

    private final int httpPort;

    public SocketIoLocalNodeAllocatorService(SocketIoProperties socketIoProperties, @Value("${server.port}") int httpPort) {
        this.socketIoProperties = socketIoProperties;
        this.httpPort = httpPort;
    }

    @Getter
    private final NetProtocol netProtocol = NetProtocol.socketio;

    private NetServerInfo netServerInfo;

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
                .externalIp(StrUtil.blankToDefault(socketIoProperties.getExternalIp(), internalIp))
                .internalIp(internalIp)
                .httpPort(httpPort)
                .httpSecure(Boolean.FALSE)
                .netExternalPort(socketIoProperties.getNetExternalPort() == null
                        ? socketIoProperties.getPort()
                        : socketIoProperties.getNetExternalPort())
                .netExternalUrl(socketIoProperties.getNetExternalUrl())
                .build();
        if (StrUtil.isBlank(netServerInfo.getNetExternalUrl())) {
            netServerInfo.setNetExternalUrl("http://" + netServerInfo.getExternalIp() + ":" + netServerInfo.getNetExternalPort());
        }
    }
}
