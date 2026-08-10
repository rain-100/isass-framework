// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.proxy.service.job;

import org.springframework.scheduling.annotation.Scheduled;
import vip.isass.framework.net.proxy.service.service.RemoveC2SMessageService;

/**
 * 定时删除 redis 旧的中转消息
 *
 * @author rain
 */
public class RemoveEarlyMessageJob {

    private final RemoveC2SMessageService removeC2SMessageService;

    public RemoveEarlyMessageJob(RemoveC2SMessageService removeC2SMessageService) {
        this.removeC2SMessageService = removeC2SMessageService;
    }

    /**
     * 每5分钟执行一次
     */
    @Scheduled(initialDelay = 5 * 60 * 1000, fixedDelay = 5 * 60 * 1000)
    public void process() {
        removeC2SMessageService.process();
    }

}
