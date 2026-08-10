// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.proxy.service.controller;

import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vip.isass.framework.common.web.Resp;
import vip.isass.framework.net.core.message.Message;
import vip.isass.framework.net.core.session.ISessionService;
import vip.isass.framework.net.proxy.service.service.GatewayToRedisMessageService;
import vip.isass.framework.net.proxy.service.service.RemoveC2SMessageService;

/**
 * @author rain
 */
@Slf4j
@RestController
@RequestMapping("/${spring.application.name}/sender")
public class NetMessageSenderController {

    private final ISessionService sessionService;
    private final GatewayToRedisMessageService gatewayToRedisMessageService;
    private final RemoveC2SMessageService removeC2SMessageService;

    public NetMessageSenderController(ISessionService sessionService, GatewayToRedisMessageService gatewayToRedisMessageService, RemoveC2SMessageService removeC2SMessageService) {
        this.sessionService = sessionService;
        this.gatewayToRedisMessageService = gatewayToRedisMessageService;
        this.removeC2SMessageService = removeC2SMessageService;
    }

    @PostMapping("/send")
    public void sendMessage(@RequestBody Message message) {
        sessionService.sendMessage(message);
    }

    @GetMapping("/pushToRedis")
    public Resp<RecordId> pushToRedis(@RequestParam("cmd") String cmd) {
        RecordId push = gatewayToRedisMessageService.push(
                Message.builder()
                        .cmd(cmd)
                        .payload(23)
                        .tags(CollUtil.newArrayList("ios"))
                        .build());
        return Resp.bizSuccess(push);
    }

    @GetMapping("/removeEarlyMessageService")
    public Resp<?> removeEarlyMessage() {
        removeC2SMessageService.process();
        return Resp.bizSuccess();
    }

}
