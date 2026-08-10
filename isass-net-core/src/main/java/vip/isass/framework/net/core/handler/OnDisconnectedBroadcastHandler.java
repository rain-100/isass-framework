// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.core.handler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import vip.isass.framework.net.core.server.Server;
import vip.isass.framework.net.core.session.ISessionService;
import vip.isass.framework.net.core.session.Session;

import java.util.Optional;

/**
 * 登录事件处理器
 *
 * @author rain
 */
@Order(-1)
@Configuration
@ConditionalOnBean(Server.class)
public class OnDisconnectedBroadcastHandler implements OnDisconnectEventHandler {

    @Autowired
    private ISessionService sessionService;

    @Override
    public void onDisconnect(Session<?> session) {
        Optional.ofNullable(sessionService.getUserId(session.getSessionId()))
                .ifPresent(userId -> {
                    sessionService.broadcastMessage("/core/user/offline", userId);
                });
        Optional.ofNullable(sessionService.getAlias(session.getSessionId()))
                .ifPresent(alias -> {
                    if (alias.startsWith("equipmentId:")) {
                        sessionService.broadcastMessage("/core/equipment/offline", alias.replace("equipmentId:", ""));
                    }
                });
    }
}
