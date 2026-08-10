// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.proxy.service.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import vip.isass.framework.net.proxy.service.service.ProxyClientCmdListeningService;

/**
 * cmd 监听任务，拉取 redis 指定的 key，取出 cmd，供 socketio 框架监听
 *
 * @author rain
 */
@Slf4j
public class ProxyClientCmdListeningJob {

    public final ProxyClientCmdListeningService proxyClientCmdListeningService;

    public ProxyClientCmdListeningJob(ProxyClientCmdListeningService proxyClientCmdListeningService) {
        this.proxyClientCmdListeningService = proxyClientCmdListeningService;
    }

    /**
     * 每隔30秒获取一次 cmd
     */
    @Scheduled(initialDelay = 10 * 1000, fixedDelay = 30 * 1000)
    public void listening() {
        proxyClientCmdListeningService.listening();
    }

}
