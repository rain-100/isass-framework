// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.support;

import lombok.extern.slf4j.Slf4j;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * @author Rain
 */
@Slf4j
public class ShardingJobUtil {

    public static void shardingConsumer(int nodeIndex,
                                        int totalNodeCount,
                                        Supplier<Integer> totalWait4ProcessDataCountSupplier,
                                        BiConsumer<Long, Long> biConsumer) {
        log.debug("分片参数: 当前节点index[{}], 总节点数[{}]", nodeIndex, totalNodeCount);

        // 获取需要处理的数据总数
        Integer dataTotalCount = totalWait4ProcessDataCountSupplier.get();

        if (dataTotalCount < 1) {
            return;
        }

        log.debug("所有节点共需要处理[{}]条记录", dataTotalCount);

        // 计算每个节点需要处理的数据量
        double dataCountOfEachNode = (double) dataTotalCount / totalNodeCount;
        log.debug("每个节点需要处理[{}]条记录", dataCountOfEachNode);

        // 若每个节点需要处理的数据量小于等于1，则直接由index为0的节点执行业务逻辑
        if (dataCountOfEachNode <= 1.0 && nodeIndex == 0) {
            long start = 1;
            long end = (long) dataTotalCount;
            log.debug("当前节点需要处理的数据位置: start[], end[]", start, end);

            // 执行业务逻辑
            biConsumer.accept(start, end);
            return;
        }

        for (int i = 0; i < totalNodeCount; i++) {
            if (i == nodeIndex) {
                long dataCountCeilOfEachNode = (long) Math.ceil(dataCountOfEachNode);

                // 计算当前节点需要处理的开始（含）与结束（含）的数据位置（下标），数据位置从1开始
                long start = 1 + i * dataCountCeilOfEachNode;
                long end = dataCountCeilOfEachNode * (i + 1);
                log.debug("当前节点需要处理的数据位置: start[{}], end[{}]", start, end);

                if (end > dataTotalCount) {
                    end = dataTotalCount;
                }

                // 执行业务逻辑
                biConsumer.accept(start, end);
                break;
            }
        }

    }

}
