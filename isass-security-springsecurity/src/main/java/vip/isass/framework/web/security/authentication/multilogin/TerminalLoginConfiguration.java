// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.authentication.multilogin;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 多端登陆配置
 *
 * @author Rain
 */
@Slf4j
@Component
public class TerminalLoginConfiguration implements SmartLifecycle {

    private static boolean IS_RUNNING = false;

    @Autowired(required = false)
    private TerminalLoginConfigLoader terminalLoginConfigLoader;

    /**
     * 多端登陆策略缓存
     * <br>
     * key: appId
     */
    private Map<String, Set<TerminalLoginConfig>> appIdIdAndTerminalLoginConfigs = Collections.emptyMap();

    /**
     * 获取多端登陆策略，匹配优先级：appGroupId、tenantId、全局
     *
     * @param appId 应用 id
     * @return 登录策略
     */
    public Collection<TerminalLoginConfig> getTerminalLoginConfig(Long appId) {
        return appId == null
                ? null
                : appIdIdAndTerminalLoginConfigs.get(appId.toString());
    }

    private void init() {
        if (terminalLoginConfigLoader == null) {
            return;
        }

        Map<String, Set<TerminalLoginConfig>> appIdAndConfigMap = new HashMap<>();
        terminalLoginConfigLoader.load()
                .forEach(c -> {
                    Set<TerminalGroup> mutexTerminals = c.getMutexTerminals();
                    if (mutexTerminals != null) {
                        for (TerminalGroup terminalGroup : mutexTerminals) {
                            for (PriorityTerminal terminal : terminalGroup.getTerminals()) {
                                appIdAndConfigMap
                                        .computeIfAbsent(terminal.getAppId().toString(), k -> new HashSet<>())
                                        .add(c);
                            }
                        }
                    }

                    Set<SameTerminalProperty> sameTerminals = c.getSameTerminals();
                    if (sameTerminals != null) {
                        for (SameTerminalProperty property : sameTerminals) {
                            appIdAndConfigMap
                                    .computeIfAbsent(property.getAppId().toString(), k -> new HashSet<>())
                                    .add(c);
                        }
                    }
                });
        appIdIdAndTerminalLoginConfigs = appIdAndConfigMap;
    }

    /**
     * 每2分钟向刷新一次
     */
    @Scheduled(initialDelay = 2 * 60 * 1000, fixedDelay = 2 * 60 * 1000)
    public void reload() {
        init();
    }

    @Override
    public void start() {
        IS_RUNNING = true;
        if (terminalLoginConfigLoader == null) {
            log.info("当前服务未添加多端登录配置加载器");
            appIdIdAndTerminalLoginConfigs = Collections.emptyMap();
            return;
        }
        init();
    }

    @Override
    public void stop() {
        IS_RUNNING = false;
    }

    @Override
    public boolean isRunning() {
        return IS_RUNNING;
    }
}
