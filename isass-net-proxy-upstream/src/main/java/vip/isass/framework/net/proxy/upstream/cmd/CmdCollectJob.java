// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.proxy.upstream.cmd;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * cmd 收集定时任务
 *
 * @author rain
 */
@Slf4j
public class CmdCollectJob {

    private final CmdCollectService cmdCollectService;

    public CmdCollectJob(CmdCollectService cmdCollectService) {
        this.cmdCollectService = cmdCollectService;
    }

    /**
     * 每2分钟向 redis 更新一次本服务的 cmd，防止 redis 数据被清了要等很长时间才恢复
     */
    @Scheduled(initialDelay = 10 * 1000, fixedDelay = 2 * 60 * 1000)
    public void cmdCollect() {
        log.debug("[kernel:net:proxy] running CmdCollectJob");
        cmdCollectService.collect();
    }
}
