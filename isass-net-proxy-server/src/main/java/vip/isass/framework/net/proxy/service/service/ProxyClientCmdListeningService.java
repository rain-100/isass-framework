// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.proxy.service.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import vip.isass.framework.net.core.NetRedisKey;
import vip.isass.framework.net.core.handler.IMessageEventRegister;
import vip.isass.framework.net.core.message.CmdCollectDto;
import vip.isass.framework.net.core.message.MessageCmd;
import vip.isass.framework.net.proxy.core.CmdRedisService;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 代理客户端 cmd 监听服务
 * 获取到所有代理客户端，即业务微服务的 cmd, 添加到 socketio 框架的监听列表
 *
 * @author rain
 */
@Slf4j
public class ProxyClientCmdListeningService {

    private final String applicationName;
    private final CmdRedisService cmdRedisService;
    private final List<IMessageEventRegister> messageEventRegisters;

    public ProxyClientCmdListeningService(@Value("${spring.application.name:}") String applicationName, CmdRedisService cmdRedisService, List<IMessageEventRegister> messageEventRegisters) {
        this.applicationName = applicationName;
        this.cmdRedisService = cmdRedisService;
        this.messageEventRegisters = messageEventRegisters;
    }

    /**
     * 正在监听的 cmd
     */
    private static final Set<String> LISTENING_COMMANDS = new HashSet<>();

    /**
     * key: cmdPrefix
     */
    private static final Map<String, MessageRedisKeyMapping> MESSAGE_REDIS_KEY_MAPPING = new ConcurrentHashMap<>();

    public static Map<String, MessageRedisKeyMapping> getMessageRedisKeyMapping() {
        return MESSAGE_REDIS_KEY_MAPPING;
    }

    /**
     * 扫描 redis 已注册的 cmd，添加到 socketio 监听队列
     */
    public void listening() {
        if (messageEventRegisters == null) {
            return;
        }
        Map<String, List<CmdCollectDto>> commandMap = cmdRedisService.findCommands();
        // 去掉网关服务服务本身的 cmd
        commandMap.remove(applicationName);

        parseRedisKeyMapping(commandMap);

        // 去掉 core 的 cmd，剩下的就是需要监听的
        Set<String> commands = commandMap.entrySet()
                .stream()
                .filter(e -> !applicationName.equals(e.getKey()))
                .map(Map.Entry::getValue)
                .flatMap(Collection::stream)
                .map(CmdCollectDto::getCmd)
                .filter(c -> !c.startsWith(MessageCmd.CORE_PREFIX))
                .collect(Collectors.toSet());

        // 取消"已经删除了的 cmd"的监听
        Collection<String> needRemoveListeningCommands = CollUtil.subtract(LISTENING_COMMANDS, commands);
        if (!needRemoveListeningCommands.isEmpty()) {
            messageEventRegisters.forEach(r -> {
                log.info("取消监听cmd: {}", needRemoveListeningCommands);
                r.removeListening(needRemoveListeningCommands);
            });
            LISTENING_COMMANDS.removeAll(needRemoveListeningCommands);
        }

        // 添加"新增的 cmd"的监听
        Collection<String> needAddListeningCommands = CollUtil.subtract(commands, LISTENING_COMMANDS);
        if (!needAddListeningCommands.isEmpty()) {
            messageEventRegisters.forEach(r -> {
                log.info("开始监听cmd: {}", needAddListeningCommands);
                r.listening(needAddListeningCommands);
            });
            LISTENING_COMMANDS.addAll(needAddListeningCommands);
        }
    }

    private void parseRedisKeyMapping(Map<String, List<CmdCollectDto>> commandMap) {
        if (commandMap.isEmpty()) {
            MESSAGE_REDIS_KEY_MAPPING.clear();
        }

        // 如果已监听的微服务，没有在本次更新的cmd集合中，则需要移除
        MESSAGE_REDIS_KEY_MAPPING.entrySet().removeIf(e -> !commandMap.containsKey(e.getValue().getServiceName()));

        int i = 0;
        for (String serviceName : commandMap.keySet()) {
            String cmdPrefix = StrUtil.SLASH + serviceName + StrUtil.SLASH;
            MESSAGE_REDIS_KEY_MAPPING.computeIfAbsent(cmdPrefix, k -> MessageRedisKeyMapping.builder()
                    .cmdPrefix(cmdPrefix)
                    .serviceName(serviceName)
                    .redisKey(NetRedisKey.REDIS_STREAM_PREFIX_KEY + serviceName)
                    .build());
            i++;
        }
    }
}
