// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.core.message;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import vip.isass.framework.net.core.session.Session;

import java.util.Collection;

/**
 * 消息对象
 * 获取消息接收方的优先顺序:
 * 1：如果有 receiverSession 或 receiverSessionId，则直接发送
 * 2：发送给所有同时满足已设置的条件
 * 3：如果 tags、tagsAny 同时设置，则忽略 tagsAny
 *
 * @author rain
 */
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    /**
     * 发送方的会话 id。在 socketio 网关转发的情况下，微服务无法得到 session 对象，使用此 sessionId 进行标识
     */
    private String senderSessionId;

    /**
     * 发送方的会话会话
     */
    @JsonIgnore
    private transient Session<?> senderSession;

    /**
     * 接收方的会话 id。
     * 如果指定了接收方的会话 id，则直接发送，不再判断标签
     */
    private String receiverSessionId;

    /**
     * 接收方的会话会话。
     */
    private transient Session<?> receiverSession;

    /**
     * 路由命令
     */
    private String cmd;

    /**
     * 待发送的消息体，payloadBytes 为空时有效
     */
    private Object payload;

    /**
     * 用户 id，如果值等于"LoginUser"，则发送给所有已登录用户，如果等于"UnLoginUser"，则发给所有未登录用户
     */
    private Collection<String> userIds;

    /**
     * 别名
     */
    private Collection<String> aliases;

    /**
     * 标签列表，并且关系
     */
    private Collection<String> tags;

    /**
     * 标签列表，或者关系
     */
    private Collection<String> tagsAny;

}
