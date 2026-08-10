// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.core.handler.manager;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.TypeUtil;
import lombok.extern.slf4j.Slf4j;
import vip.isass.framework.common.converter.ConvertUtil;
import vip.isass.framework.net.core.handler.OnAnyMessageEventHandler;
import vip.isass.framework.net.core.handler.OnConnectEventHandler;
import vip.isass.framework.net.core.handler.OnDisconnectEventHandler;
import vip.isass.framework.net.core.handler.OnErrorEventHandler;
import vip.isass.framework.net.core.handler.OnMessageEventHandler;
import vip.isass.framework.net.core.message.Message;
import vip.isass.framework.net.core.message.MessageCmd;
import vip.isass.framework.net.core.session.ISessionService;
import vip.isass.framework.net.core.session.Session;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 事件管理器
 *
 * @author Rain
 */
@Slf4j
public class EventManager implements IEventManager {

    private final ISessionService sessionService;

    private final List<OnConnectEventHandler> onConnectEventHandlers;

    private final List<OnDisconnectEventHandler> onDisconnectEventHandlers;

    private final List<OnErrorEventHandler> onErrorEventHandlers;

    private final List<OnAnyMessageEventHandler<?>> onAnyMessageEventHandlers;

    private final List<OnMessageEventHandler<?>> onMessageEventHandlers;

    private final Map<String, List<OnMessageEventHandler<?>>> onMessageEventHandlerMap;

    public EventManager(ISessionService sessionService,
                        List<OnConnectEventHandler> onConnectEventHandlers,
                        List<OnDisconnectEventHandler> onDisconnectEventHandlers,
                        List<OnErrorEventHandler> onErrorEventHandlers,
                        List<OnAnyMessageEventHandler<?>> onAnyMessageEventHandlers,
                        List<OnMessageEventHandler<?>> onMessageEventHandlers) {
        this.sessionService = sessionService;
        this.onConnectEventHandlers = onConnectEventHandlers;
        this.onDisconnectEventHandlers = onDisconnectEventHandlers;
        this.onErrorEventHandlers = onErrorEventHandlers;
        this.onAnyMessageEventHandlers = onAnyMessageEventHandlers;
        this.onMessageEventHandlers = onMessageEventHandlers;

        if (onMessageEventHandlers == null) {
            this.onMessageEventHandlerMap = Collections.emptyMap();
        } else {
            this.onMessageEventHandlerMap = MapUtil.newHashMap(onMessageEventHandlers.size());
            onMessageEventHandlers
                    .forEach(h -> onMessageEventHandlerMap
                            .computeIfAbsent(StrUtil.nullToEmpty(h.getCmd()), s -> new ArrayList<>())
                            .add(h));
        }
    }

    @Override
    public void onConnect(Session<?> session) {
        sessionService.addSession(session);
        if (onConnectEventHandlers != null) {
            onConnectEventHandlers.forEach(h -> h.onConnect(session));
        }

        log.debug("[{}]客户端连接，客户端ip：{}", session.getServerType().getSimpleName(), session.getRemoteIp());
    }

    @Override
    public void onDisconnect(Session<?> session) {
        if (onDisconnectEventHandlers != null) {
            try {
                onDisconnectEventHandlers.forEach(h -> h.onDisconnect(session));
            } catch (Exception e) {
                log.error("执行 OnDisconnectEventHandler 异常：{}", e.getMessage(), e);
            }
        }
        sessionService.removeSession(session);
        log.debug("[{}]客户端断连，客户端ip：{}", session.getServerType().getSimpleName(), session.getRemoteIp());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> void onMessage(Message message) {
        log.trace("收到客户端消息: cmd[{}] payload[{}]", message.getCmd(), message.getPayload());

        List<OnMessageEventHandler<?>> handlers = StrUtil.isBlank(message.getCmd())
                ? onMessageEventHandlers
                : onMessageEventHandlerMap.get(message.getCmd());

        Map<Type, T> convertedPayloadMap = MapUtil.newHashMap(2);

        if (handlers != null) {
            for (OnMessageEventHandler<?> handler : handlers) {
                OnMessageEventHandler<T> h = (OnMessageEventHandler<T>) handler;
                T convertedPayload;
                try {
                    Type actualType = TypeUtil.toParameterizedType(h.getClass()).getActualTypeArguments()[0];
                    convertedPayload = convertedPayloadMap.computeIfAbsent(
                            actualType,
                            k -> ConvertUtil.convert(k, message.getPayload()));
                } catch (Exception e) {
                    String errorMessage = StrUtil.format(
                            "反序列化消息失败：cmd[{}],error[{}]",
                            message.getCmd(),
                            e.getMessage());
                    log.error(errorMessage, e);
                    replyMessage(message, MessageCmd.ERROR, errorMessage);
                    continue;
                }

                try {
                    Object process = h.onMessage(message, convertedPayload);
                    if (process != null) {
                        replyMessage(message, message.getCmd(), process);
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    replyMessage(message, MessageCmd.ERROR, e.getMessage());
                }
            }
        }

        if (onAnyMessageEventHandlers != null) {
            for (OnAnyMessageEventHandler<?> handler : onAnyMessageEventHandlers) {
                OnAnyMessageEventHandler<T> h = (OnAnyMessageEventHandler<T>) handler;
                T convertedPayload;
                try {
                    Type actualType = TypeUtil.toParameterizedType(h.getClass()).getActualTypeArguments()[0];
                    convertedPayload = convertedPayloadMap.computeIfAbsent(
                            actualType,
                            k -> ConvertUtil.convert(k, message.getPayload()));
                } catch (Exception e) {
                    String errorMessage = StrUtil.format(
                            "反序列化消息失败：cmd[{}],error[{}]",
                            message.getCmd(),
                            e.getMessage());
                    log.error(errorMessage, e);
                    replyMessage(message, MessageCmd.ERROR, errorMessage);
                    continue;
                }

                try {
                    Object process = h.onMessage(message, convertedPayload);
                    if (process != null) {
                        replyMessage(message, message.getCmd(), process);
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    replyMessage(message, MessageCmd.ERROR, e.getMessage());
                }
            }
        }
        convertedPayloadMap.clear();
    }

    @Override
    public void onError(Session<?> session, Throwable throwable) {
        if (onErrorEventHandlers != null) {
            onErrorEventHandlers.forEach(h -> h.onError(session, null, null, throwable));
        }
        Throwable e = ExceptionUtil.unwrap(throwable);
        session.sendMessage(MessageCmd.ERROR, "发生异常" + e.getMessage());
        log.error("[{}]sessionId[{}]发生异常[{}]，将关闭该连接",
                session.getServerType().getSimpleName(),
                session.getSessionId(),
                e.getMessage());
        sessionService.removeSession(session);
        session.close();
    }

    private void replyMessage(Message message, String cmd, Object payload) {
        if (message.getSenderSession() != null) {
            message.getSenderSession().sendMessage(cmd, payload);
            return;
        }

        if (StrUtil.isBlank(message.getSenderSessionId())) {
            return;
        }

        sessionService.sendMessage(Message.builder()
                .receiverSession(message.getSenderSession())
                .receiverSessionId(message.getSenderSessionId())
                .cmd(cmd)
                .payload(payload)
                .build());
    }

}
