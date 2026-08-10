// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.core.handler.manager;

import org.springframework.beans.factory.InitializingBean;
import vip.isass.framework.net.core.handler.IMessageEventRegister;
import vip.isass.framework.net.core.handler.OnMessageEventHandler;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 消息事件处理注册管理
 *
 * @author rain
 */
public class MessageEventRegisterManager implements InitializingBean {

    private final List<IMessageEventRegister> messageEventRegisters;

    private final List<OnMessageEventHandler<?>> onMessageEventHandlers;

    public MessageEventRegisterManager(List<IMessageEventRegister> messageEventRegisters,
                                        List<OnMessageEventHandler<?>> onMessageEventHandlers) {
        this.messageEventRegisters = messageEventRegisters;
        this.onMessageEventHandlers = onMessageEventHandlers;
    }

    @Override
    public void afterPropertiesSet() {
        register();
    }

    private void register() {
        if (messageEventRegisters == null || onMessageEventHandlers == null) {
            return;
        }

        messageEventRegisters.forEach(r -> r.listening(
            onMessageEventHandlers
                .stream()
                .map(OnMessageEventHandler::getCmd)
                .collect(Collectors.toSet())
            )
        );
    }

}
