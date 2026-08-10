// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.core.server;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 服务端启动管理器
 *
 * @author rain
 */
@Slf4j
@Configuration
@ConditionalOnBean(Server.class)
public class ServerStartupManager implements SmartLifecycle {

    private static boolean IS_RUNNING = false;

    @Resource
    private List<Server> servers;

    @Override
    public void start() {
        if (IS_RUNNING || servers == null) {
            return;
        }

        servers.forEach(s -> {
            log.info("正在启动 net 模块[{}] 监听地址[{}]", s.getClass().getSimpleName(), s.getListeningAddress());
            s.start();
        });

        IS_RUNNING = true;
    }

    @Override
    public void stop() {
        if (servers == null) {
            return;
        }

        servers.forEach(s -> {
            log.info("正在关闭 net 模块[{}]", s.getClass().getSimpleName());
            s.stop();
        });

        IS_RUNNING = false;
    }

    @Override
    public boolean isRunning() {
        return IS_RUNNING;
    }

}
