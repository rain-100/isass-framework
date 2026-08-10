// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.authentication.multilogin;

import cn.hutool.core.collection.CollUtil;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 终端配置
 * 如果启用多终端配置，需依赖 redis 记录已登录终端
 *
 * @author Rain
 */
@Setter
@Deprecated
@Accessors(chain = true)
public class TerminalOnlineProperties {
    /**
     * 多终端登录配置 key
     */
    public static final String TERMINAL_ONLINE_PROPERTIES_KEY = "TerminalOnline";

    public static final String ENABLE_ALL_TERMINAL_SAME_TIME_ONLINE = "enableAllTerminalSameTimeOnline";

    public static final String ENABLE_FORCE_OFFLINE_IF_TERMINAL_NOT_CONFIGURED = "enableForceOfflineIfTerminalNotConfigured";

    public static final String DIRECT_ONLINE_TERMINALS = "directOnlineTerminals";

    public static final String MUTEX_TERMINAL = "mutexTerminals";

    public static final String SAME_TERMINALS_ONLINE_AT_SAME_TIME_TERMINALS = "sameTerminalsOnlineAtSameTimeTerminals";

    /**
     * 是否允许所有终端同时在线,是则放弃所有判断，直接在线
     */
    private Boolean enableAllTerminalSameTimeOnline;

    /**
     * 是否允许没配置的终端，强制下线
     */
    private Boolean enableForceOfflineIfTerminalNotConfigured;

    /**
     * 直接上线终端，不需要管其他终端有没有在线的终端，如果需要同端多登，还需要配置 sameEndsOnline
     */
    private List<String> directOnlineTerminals;

    /**
     * 互斥终端，只能其中一个在线
     */
    private List<String> mutexTerminals;

    /**
     * 同端多登，可以多台同一终端类型同时在线的终端
     */
    private List<String> sameTerminalsOnlineAtSameTimeTerminals;

    public Boolean getEnableAllTerminalSameTimeOnline() {
        return enableAllTerminalSameTimeOnline == null
                ? true
                : enableAllTerminalSameTimeOnline;
    }

    public Boolean getEnableForceOfflineIfTerminalNotConfigured() {
        return enableForceOfflineIfTerminalNotConfigured == null
                ? false
                : enableForceOfflineIfTerminalNotConfigured;
    }

    public List<String> getDirectOnlineTerminals() {
        return directOnlineTerminals == null
                ? CollUtil.newArrayList("ipad", "web-pc", "web-h5", "web-apidoc", "windows", "mac")
                : directOnlineTerminals;
    }

    public List<String> getMutexTerminals() {
        return mutexTerminals == null
                ? CollUtil.newArrayList("ios", "android")
                : mutexTerminals;
    }

    public List<String> getSameTerminalsOnlineAtSameTimeTerminals() {
        return sameTerminalsOnlineAtSameTimeTerminals == null
                ? CollUtil.newArrayList("web-pc", "web-h5", "web-apidoc")
                : sameTerminalsOnlineAtSameTimeTerminals;
    }

}
