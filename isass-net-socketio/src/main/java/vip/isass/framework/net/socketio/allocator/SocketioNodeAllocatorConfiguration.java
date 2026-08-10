// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.socketio.allocator;

import cn.hutool.core.util.StrUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vip.isass.framework.net.core.server.NetProtocol;
import vip.isass.framework.net.core.server.NetServerInfo;
import vip.isass.framework.net.core.server.allocator.INodeAllocatorService;
import vip.isass.framework.net.proxy.core.ConsistentHashNodeAllocatorService;

/**
 * @author isass
 */
@Configuration
@ConditionalOnProperty(name = {"kernel.net.enabled", "kernel.net.proxy.enabled"}, havingValue = "true")
public class SocketioNodeAllocatorConfiguration {

    @Bean
    public INodeAllocatorService socketioConsistentHashNodeAllocatorService() {
        return new ConsistentHashNodeAllocatorService(NetProtocol.socketio) {

            @Override
            public void formatNetExternalUrl(NetServerInfo netServerInfo) {
                if (StrUtil.isBlank(netServerInfo.getNetExternalUrl())) {
                    netServerInfo.setNetExternalUrl("http://" + netServerInfo.getExternalIp() + ":" + netServerInfo.getNetExternalPort());
                }
            }
        };
    }
}
