// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.netty.request;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import vip.isass.framework.net.netty.packet.IPacket;
import vip.isass.framework.net.core.session.Session;
import vip.isass.framework.common.support.JsonUtil;

/**
 * 一个网络包请求就是一个系统的事件.类似一个task任务
 *
 * @author Rain
 */
@Slf4j
@Getter
@Accessors(chain = true)
public class Request<P extends IPacket, S extends Session> {

    /**
     * 分组
     */
    private String group;

    private P packet;

    private S session;

    private Protocol requestProtocol;

    public Request(P packet, S session, Protocol requestProtocol) {
        this.packet = packet;
        this.session = session;
        this.requestProtocol = requestProtocol;
    }

    public Request(String group, P packet, S session, Protocol requestProtocol) {
        this.group = group;
        this.packet = packet;
        this.session = session;
        this.requestProtocol = requestProtocol;
    }

    @SneakyThrows
    public void sendResponse(IPacket packet) {
        if (requestProtocol == Protocol.WEBSOCKET) {
            String json = JsonUtil.DEFAULT_INSTANCE.writeValueAsString(packet);
//            this.session.sendMessage(new TextWebSocketFrame(json))Z
        } else if (requestProtocol == Protocol.TCP) {
            this.session.sendMessage(packet.getCmd(), packet);
        }
    }

    public enum Protocol {
        TCP,
        WEBSOCKET;
    }

    @Override
    public String toString() {
        return new StringBuilder("{")
            .append("\"group\":\"")
            .append(group).append('\"')
            .append(",\"packet\":")
            .append(packet)
            .append(",\"session\":")
            .append(session)
            .append(",\"requestProtocol\":")
            .append(requestProtocol)
            .append('}')
            .toString();
    }
}
