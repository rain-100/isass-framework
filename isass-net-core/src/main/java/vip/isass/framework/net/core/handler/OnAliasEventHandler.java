// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.core.handler;

import cn.hutool.core.lang.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import vip.isass.framework.net.core.message.Message;
import vip.isass.framework.net.core.message.MessageCmd;
import vip.isass.framework.net.core.server.Server;
import vip.isass.framework.net.core.session.ISessionService;

/**
 * 登录事件处理器
 *
 * @author rain
 */
@Order(-1)
@Configuration
@ConditionalOnBean(Server.class)
public class OnAliasEventHandler implements OnMessageEventHandler<String> {

    @Autowired
    private ISessionService sessionService;

    @Override
    public String getCmd() {
        return MessageCmd.ALIAS;
    }

    @Override
    public Object onMessage(Message message, String alias) {
        Assert.notNull(alias, "alias can not be null");
        sessionService.setAlias(message.getSenderSessionId(), alias);
        if (alias.startsWith("equipmentId:")) {
            sessionService.broadcastMessage("/core/equipment/online", alias.replace("equipmentId:", ""));
        }
        return null;
    }

}
