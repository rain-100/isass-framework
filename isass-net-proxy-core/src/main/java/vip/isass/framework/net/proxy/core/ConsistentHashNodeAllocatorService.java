// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.proxy.core;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.ConsistentHash;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.scheduling.annotation.Scheduled;
import vip.isass.framework.net.core.server.NetProtocol;
import vip.isass.framework.net.core.server.NetServerInfo;
import vip.isass.framework.net.core.server.allocator.INodeAllocatorService;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 一致性 hash 算法的实例存储，在使用网关代理时，需要用到此算法分配网关节点给客户端连接
 */
@Slf4j
public abstract class ConsistentHashNodeAllocatorService implements INodeAllocatorService {

    /**
     * 总节点数量不低于，非实际总节点数。
     * <p>
     * 在虚拟节点数没配置或为 -1 的情况下，会根据实际物理节点数和总节点数，算出一个不低于总节点数的虚拟节点数。总节点数量会保持在这个数量之上
     * </p>
     */
    @Value("${kernel.net.proxy.consistentHash.totalNodeAbove:1000}")
    private int totalNodeAbove = 1000;

    /**
     * 每个物理节点对应的虚拟节点数量
     */
    @Value("${kernel.net.proxy.consistentHash.virtualNodeCount:-1}")
    private int virtualNodeCount = -1;

    @Getter
    private final NetProtocol netProtocol;

    public ConsistentHashNodeAllocatorService(NetProtocol netProtocol) {
        this.netProtocol = netProtocol;
    }

    @Resource
    private DiscoveryClient discoveryClient;

    private Map<String, NetServerInfo> serviceInstanceMap = new HashMap<>();

    private ConsistentHash<String> hashServer;

    public NetServerInfo allocate(String clientIp) {
        Assert.isFalse(serviceInstanceMap.isEmpty() || hashServer == null,
                () -> new RuntimeException(StrUtil.format("未查询到可用的[{}]网关节点，请稍后再试", netProtocol)));
        String instanceKey = StrUtil.isBlank(clientIp)
                ? hashServer.get(RandomUtil.randomString(2))
                : hashServer.get(clientIp);
        NetServerInfo netServerInfo = serviceInstanceMap.get(instanceKey);
        if (netServerInfo == null) {
            throw new RuntimeException(StrUtil.format("[{}]节点分配失败，请稍后再试", netProtocol));
        }
        return netServerInfo;
    }

    @Override
    public Collection<NetServerInfo> getAll() {
        return serviceInstanceMap.values();
    }

    /**
     * 每隔 10s 刷新一次节点
     */
    @Scheduled(fixedDelay = 10 * 1000)
    public void refreshNode() {
        List<ServiceInstance> instances = discoveryClient.getInstances(netProtocol.getServiceName());
        Map<String, NetServerInfo> infoMap = new HashMap<>();
        for (ServiceInstance instance : instances) {
            Map<String, String> metadata = instance.getMetadata();
            NetServerInfo serverInfo = NetServerInfo.builder()
                    .netProtocol(netProtocol)
                    .externalIp(metadata.get("externalIp"))
                    .internalIp(MapUtil.getStr(metadata, "internalIp", instance.getHost()))
                    .httpPort(MapUtil.getInt(metadata, "httpPort", instance.getPort()))
                    .httpSecure(MapUtil.getBool(metadata, "isSecure", instance.isSecure()))
                    .netExternalPort(MapUtil.getInt(metadata, "netExternalPort"))
                    .netExternalUrl(metadata.get("netExternalUrl"))
                    .build();
            if (StrUtil.isBlank(serverInfo.getExternalIp())) {
                serverInfo.setExternalIp(serverInfo.getInternalIp());
            }
            if (StrUtil.isBlank(serverInfo.getNetExternalUrl())) {
                formatNetExternalUrl(serverInfo);
            }
            infoMap.put(serverInfo.getNetExternalUrl(), serverInfo);
        }

        if (checkModify(infoMap)) {
            int numberOfReplicas = calculateNumberOfReplicas(totalNodeAbove, virtualNodeCount, infoMap.size());
            this.serviceInstanceMap = infoMap;
            hashServer = new ConsistentHash<>(numberOfReplicas, this.serviceInstanceMap.keySet());
        }
    }

    /**
     * 检查服务节点是否有变更
     *
     * @param latestNodeMap 最新的节点
     * @return 是否有变更
     */
    private boolean checkModify(Map<String, NetServerInfo> latestNodeMap) {
        if (latestNodeMap.size() != serviceInstanceMap.size()) {
            return true;
        }
        for (Map.Entry<String, NetServerInfo> entry : latestNodeMap.entrySet()) {
            NetServerInfo netServerInfo = serviceInstanceMap.get(entry.getKey());
            if (netServerInfo == null) {
                return true;
            }
            // 全部相等，才没有变更
            boolean isAllEquals = BooleanUtil.and(
                    Objects.equals(netServerInfo.getNetProtocol(), entry.getValue().getNetProtocol()),
                    Objects.equals(netServerInfo.getExternalIp(), entry.getValue().getExternalIp()),
                    Objects.equals(netServerInfo.getInternalIp(), entry.getValue().getInternalIp()),
                    Objects.equals(netServerInfo.getHttpPort(), entry.getValue().getHttpPort()),
                    Objects.equals(netServerInfo.getHttpSecure(), entry.getValue().getHttpSecure()),
                    Objects.equals(netServerInfo.getNetExternalPort(), entry.getValue().getNetExternalPort()),
                    Objects.equals(netServerInfo.getNetExternalUrl(), entry.getValue().getNetExternalUrl()));
            if (!isAllEquals) {
                return true;
            }
        }
        return false;
    }

    private static int calculateNumberOfReplicas(int totalNodeAbove, int virtualNodeCount, int nodeSize) {
        return nodeSize == 0
                ? 0
                : virtualNodeCount >= 0
                ? ++virtualNodeCount
                : new BigDecimal(totalNodeAbove)
                .divide(new BigDecimal(nodeSize), RoundingMode.CEILING)
                .intValue();
    }

    public static void main(String[] args) {
        System.out.println(calculateNumberOfReplicas(100, 0, 3));
    }

    public abstract void formatNetExternalUrl(NetServerInfo netServerInfo);

}
