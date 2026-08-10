// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.core.handler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import vip.isass.framework.common.web.Resp;
import vip.isass.framework.net.core.message.Message;
import vip.isass.framework.net.core.message.MessageCmd;
import vip.isass.framework.net.core.server.Server;
import vip.isass.framework.net.core.session.ISessionService;

/**
 * 登录事件处理器
 *
 * @author rain
 */
@Configuration
@ConditionalOnBean(Server.class)
public class OnLogoutEventHandler implements OnMessageEventHandler<String> {

    @Autowired
    private ISessionService sessionService;

    @Override
    public String getCmd() {
        return MessageCmd.LOGOUT;
    }

    @Override
    public Object onMessage(Message message, String token) {
        sessionService.removeUserId(message.getSenderSessionId());
        return Resp.bizSuccess("登出成功");
    }

}
