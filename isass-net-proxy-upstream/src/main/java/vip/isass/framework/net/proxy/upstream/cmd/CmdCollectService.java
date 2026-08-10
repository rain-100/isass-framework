// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.proxy.upstream.cmd;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.lock.annotation.Lock4j;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import vip.isass.framework.common.support.LocalDateTimeUtil;
import vip.isass.framework.common.support.SystemClock;
import vip.isass.framework.net.core.handler.OnMessageEventHandler;
import vip.isass.framework.net.core.message.CmdCollectDto;
import vip.isass.framework.net.proxy.core.CmdRedisService;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * cmd 收集服务
 * 扫描本微服务的 onMessageEventHandler 集合，收集 cmd，保存到 redis
 *
 * @author rain
 */
@Slf4j
public class CmdCollectService {

    private final CmdRedisService cmdRedisService;

    private final List<OnMessageEventHandler<?>> onMessageEventHandlers;

    private final String applicationName;

    public CmdCollectService(CmdRedisService cmdRedisService,
                             @Autowired(required = false) List<OnMessageEventHandler<?>> onMessageEventHandlers,
                             @Value("${spring.application.name:}") String applicationName) {
        this.cmdRedisService = cmdRedisService;
        this.onMessageEventHandlers = onMessageEventHandlers;
        this.applicationName = applicationName;
    }

    @Lock4j(name = "CmdCollectService:", keys = "#applicationName", acquireTimeout = 10_000, expire = 30_000)
    public void collect(List<OnMessageEventHandler<?>> onMessageEventHandlers, String applicationName) {
        if (applicationName.isEmpty()) {
            log.error("未配置spring.application.name，net 模块 cmd 收集服务无法执行");
            return;
        }

        Long now = SystemClock.now();
        Collection<CmdCollectDto> cmdCollectDtoList = onMessageEventHandlers.stream()
                .map(OnMessageEventHandler::getCmd)
                .map(c -> CmdCollectDto.builder().cmd(c).collectTime(now).build())
                .collect(Collectors.toSet());

        List<CmdCollectDto> commands = cmdRedisService.findCommands(applicationName);
        if (CollUtil.isNotEmpty(commands)) {
            // 若 cmd 的收集时间超过了2天，说明已经没有实例使用该 cmd,应该删除
            long threeDaysAgo = LocalDateTimeUtil.toMilliseconds(LocalDateTimeUtil.now().minusDays(2));
            commands = commands.stream()
                    .filter(c -> c.getCollectTime() > threeDaysAgo)
                    .collect(Collectors.toList());

            cmdCollectDtoList.addAll(commands);
        }

        cmdRedisService.put(applicationName, cmdCollectDtoList);
    }

    public void collect() {
        if (onMessageEventHandlers == null) {
            return;
        }
        collect(onMessageEventHandlers, applicationName);
    }
}
