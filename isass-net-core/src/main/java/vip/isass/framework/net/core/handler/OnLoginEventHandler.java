// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.core.handler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import vip.isass.framework.common.security.jwt.JwtInfo;
import vip.isass.framework.common.security.jwt.JwtUtil;
import vip.isass.framework.common.web.Resp;
import vip.isass.framework.net.core.message.Message;
import vip.isass.framework.net.core.message.MessageCmd;
import vip.isass.framework.net.core.server.Server;
import vip.isass.framework.net.core.session.ISessionService;

import java.util.Collections;

/**
 * 登录事件处理器
 *
 * @author rain
 */
@Order(-1)
@Configuration
@ConditionalOnBean(Server.class)
public class OnLoginEventHandler implements OnMessageEventHandler<String> {

    @Value("${isass.security.jwt.secret:" + JwtUtil.DEFAULT_SECRET + "}")
    private String secret;

    @Autowired
    private ISessionService sessionService;

    @Override
    public String getCmd() {
        return MessageCmd.LOGIN;
    }

    @Override
    public Object onMessage(Message message, String token) {
        JwtInfo jwtInfo = JwtUtil.parse(token, secret);
        sessionService.setUserId(message.getSenderSessionId(), String.valueOf(jwtInfo.getUid()));
        Long appId = jwtInfo.getAid();
        if (appId != null) {
            sessionService.setTags(message.getSenderSessionId(), Collections.singleton("appId:" + appId));
        }
        return Resp.bizSuccess(jwtInfo);
    }

}
