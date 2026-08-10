// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.core.allocator;

import cn.hutool.cache.Cache;
import cn.hutool.cache.CacheUtil;
import cn.hutool.core.lang.Assert;
import vip.isass.framework.net.core.server.NetServerInfo;
import vip.isass.framework.net.core.server.allocator.INodeAllocatorService;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 节点分配器服务
 */
public class AllocatorService {

    private final Map<String, INodeAllocatorService> nodeAllocatorServiceMap;

    private static final Cache<NetServerInfo, String> SERVER_INFO_CACHE = CacheUtil.newTimedCache(TimeUnit.DAYS.toMillis(1));

    public AllocatorService(List<INodeAllocatorService> nodeAllocatorServices) {
        nodeAllocatorServiceMap = nodeAllocatorServices.stream()
                .collect(Collectors.toMap(s -> s.getNetProtocol().getServiceName(), Function.identity()));
    }

    /**
     * 分配节点
     * <p>
     * 优先根据用户 id 分配，其次客户端 ip
     * </p>
     *
     * @param serverName 服务名
     * @param clientIp   客户端 ip
     * @return 分配到的节点
     */
    public String allocateAccessUrl(String serverName, String clientIp) {
        INodeAllocatorService nodeAllocatorService = nodeAllocatorServiceMap.get(serverName);
        Assert.notNull(nodeAllocatorService, "找不到服务[{}]的节点分配器", serverName);
        return nodeAllocatorService.allocateAccessUrl(clientIp);
    }

}
