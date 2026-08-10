// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.proxy.service.handler.net;

import cn.hutool.core.lang.Assert;
import org.springframework.beans.factory.annotation.Value;
import vip.isass.framework.net.core.handler.OnAnyMessageEventHandler;
import vip.isass.framework.net.core.message.Message;
import vip.isass.framework.net.core.message.MessageCmd;
import vip.isass.framework.net.proxy.service.service.GatewayToRedisMessageService;

/**
 * 消息分发器，收到客户端消息时，应该由网关直接消费，还是分发到各微服务
 *
 * @author Administrator
 */
public class MessageDispatcherHandler implements OnAnyMessageEventHandler<Object> {

    private final String msPrefix;
    private final GatewayToRedisMessageService gatewayToRedisMessageService;

    public MessageDispatcherHandler(@Value("${spring.application.name:}") String applicationName, GatewayToRedisMessageService gatewayToRedisMessageService) {
        Assert.notBlank(applicationName, "未配置spring.application.name，启动失败");
        this.msPrefix = "/" + applicationName + "/";
        this.gatewayToRedisMessageService = gatewayToRedisMessageService;
    }

    @Override
    public Object onMessage(Message message, Object payload) {
        // 如果是本微服务或 core 的路由，已经由 eventManager 进行过处理
        if (message.getCmd().startsWith(msPrefix) || message.getCmd().startsWith(MessageCmd.CORE_PREFIX)) {
            return null;
        }

        // 属于其他微服务的消息，把消息推送到 redis，让具体的微服务处理消息
        gatewayToRedisMessageService.push(Message.builder()
                .senderSessionId(message.getSenderSessionId())
                .senderSession(message.getSenderSession())
                .cmd(message.getCmd())
                .payload(payload)
                .build());
        return null;
    }

}
