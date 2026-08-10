// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.core.server.allocator;

import vip.isass.framework.net.core.server.NetProtocol;
import vip.isass.framework.net.core.server.NetServerInfo;

import java.util.Collection;

/**
 * 节点分配器
 */
public interface INodeAllocatorService {

    NetServerInfo allocate(String clientIp);

    /**
     * 分配接入 url
     *
     * @param clientIp 客户端 ip
     * @return 前端接入的 url
     */
    default String allocateAccessUrl(String clientIp) {
        NetServerInfo info = allocate(clientIp);
        return info.getNetExternalUrl();
    }

    Collection<NetServerInfo> getAll();

    NetProtocol getNetProtocol();

}
