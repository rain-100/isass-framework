// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.core.session;

import cn.hutool.core.lang.Assert;
import vip.isass.framework.net.core.message.Message;

import jakarta.annotation.Nonnull;
import java.util.Collection;
import java.util.Map;

/**
 * 会话服务，记录已经建立链接的会话，上层应用使用本接口发送消息给对端
 * 删除 session 时，会自动调用 session 的 close 方法
 *
 * @author Rain
 */
public interface ISessionService {

    /**
     * 默认的命名空间
     */
    String DEFAULT_NAMESPACE = "default";

    // region session

    /**
     * 新增会话
     * todo 不应该暴露此接口给应用调用
     *
     * @param session 会话
     */
    void addSession(Session<?> session);

    /**
     * 删除会话
     * todo 不应该暴露此接口给应用调用
     *
     * @param session 会话
     */
    default void removeSession(Session<?> session) {
        Assert.notNull(session, "session 不能为 null");
        removeSession(session.getSessionId());
    }

    /**
     * 根据会话 id 删除会话
     * todo 不应该暴露此接口给应用调用
     *
     * @param sessionId 会话 id
     * @return 被删除的会话
     */
    Session<?> removeSession(String sessionId);

    /**
     * 根据链接通道获取会话
     * todo 不应该暴露此接口给应用调用
     *
     * @param sessionId 会话 id
     * @return 会话
     */
    Session<?> getSessionById(String sessionId);

    /**
     * 获取会话 id
     *
     * @param userId 用户 id
     * @return 会话 id 集合
     */
    // Collection<String> findSessionIds(String userId);

    /**
     * 获取所有会话
     *
     * @return 会话集合
     */
    Collection<Session<?>> findAllSessions();

    /**
     * 获取 session 所有信息，用于调试
     *
     * @return 会话信息集合
     */
    SessionInfoCollection getSessionInfoCollection();

    // endregion

    // region user id

    /**
     * 获取用户 id
     *
     * @return 用户 id
     */
    String getUserId(String sessionId);

    /**
     * 设置用户 id
     *
     * @param userId 用户 id
     */
    void setUserId(String sessionId, String userId);

    /**
     * 删除指定会话绑定的用户
     *
     * @param sessionId 会话 id
     */
    void removeUserId(String sessionId);

    /**
     * 批量判断用户是否在线
     *
     * @param userIds 用户 id 集合
     * @return 每个用户是否在线
     */
    Map<String, Boolean> isOnline(Collection<String> userIds);

    // endregion

    // region alias

    /**
     * 获取别名
     *
     * @return 别名
     */
    String getAlias(String sessionId);


    /**
     * 获取别名
     *
     * @param alias 别名
     * @return 会话 id 列表
     */
    Collection<String> findSessionIdsByAlias(String alias);

    /**
     * 查询已有别名
     *
     * @param prefix 前缀
     * @return 符合条件的已有别名列表
     */
    Collection<String> findAliases(String prefix);

    /**
     * 设置别名
     *
     * @param sessionId 会话
     * @param alias     别名
     */
    void setAlias(String sessionId, String alias);

    /**
     * 删除会话的别名
     *
     * @param sessionId 会话 id
     */
    void removeAlias(String sessionId);

    // endregion

    // region tag

    /**
     * 获取标签
     *
     * @return 标签列表
     */
    Collection<String> findTags(String sessionId);

    /**
     * 根据用户获取标签
     *
     * @return 标签列表
     */
    Collection<String> findTagsByUserId(String userId);

    /**
     * 查找符合所有标签的会话
     *
     * @param tags 标签集合
     * @return 符合条件的会话集合
     */
    // Collection<String> findSessionIds(Collection<String> tags);

    /**
     * 根据标签查找会话
     *
     * @param tags 标签集合
     * @return 符合条件的会话集合
     */
    Collection<String> findSessionsByAnyMatchTags(Collection<String> tags);

    /**
     * 判断会话是否拥有任意给定的标签
     *
     * @param sessionId 会话 id
     * @param tags      给定的标签
     * @return 是否拥有标签
     */
    boolean containAnyTag(@Nonnull String sessionId, @Nonnull Collection<String> tags);

    /**
     * 判断会话是否拥有所有给定的标签
     *
     * @param sessionId 会话 id
     * @param tags      给定的标签
     * @return 是否拥有标签
     */
    boolean containAllTags(String sessionId, Collection<String> tags);

    /**
     * 设置标签
     *
     * @param tags 标签集合
     */
    void setTags(String sessionId, Collection<String> tags);

    /**
     * 设置标签
     *
     * @param userId 用户 id
     */
    void setTagsByUserId(String userId, Collection<String> tags);

    /**
     * 添加标签
     *
     * @param tags 标签集合
     */
    void addTags(String sessionId, Collection<String> tags);

    /**
     * 添加标签
     *
     * @param userId 用户 id
     */
    void addTagsByUserId(String userId, Collection<String> tags);

    /**
     * 删除标签
     *
     * @param sessionId 会话 id
     */
    void removeTags(String sessionId);

    /**
     * 删除标签
     *
     * @param sessionId 会话 id
     * @param tags      标签集合
     */
    void removeTags(String sessionId, Collection<String> tags);

    /**
     * 删除标签
     *
     * @param userId 用户 id
     */
    void removeTagsByUserId(String userId, Collection<String> tags);

    // endregion

    // region message

    /**
     * 广播消息
     *
     * @param cmd     路由命令
     * @param payload 消息内容
     */
    void broadcastMessage(String cmd, Object payload);

    /**
     * 发送消息给指定的用户
     *
     * @param cmd     路由命令
     * @param payload 消息内容
     * @param userId  用户 id
     */
    void sendMessageByUserId(String cmd, Object payload, String userId);

    /**
     * 发送消息给指定的用户集合
     *
     * @param cmd     路由命令
     * @param payload 消息内容
     * @param userIds 用户 id 集合
     */
    void sendMessageByUserIds(String cmd, Object payload, Collection<String> userIds);

    /**
     * 发送消息给已登录用户
     *
     * @param cmd     路由命令
     * @param payload 消息内容
     */
    void sendMessageToLoginUsers(String cmd, Object payload);

    /**
     * 发送消息给拥有指定标签的用户
     *
     * @param cmd     路由命令
     * @param payload 消息内容
     * @param alias   标签
     */
    void sendMessageByAlias(String cmd, Object payload, String alias);

    /**
     * 发送消息给拥有指定标签的用户
     *
     * @param cmd     路由命令
     * @param payload 消息内容
     * @param aliases 标签集合
     */
    void sendMessageByAlias(String cmd, Object payload, Collection<String> aliases);

    /**
     * 发送消息给拥有指定标签的用户
     *
     * @param cmd     路由命令
     * @param payload 消息内容
     * @param tag     标签
     */
    void sendMessageByTag(String cmd, Object payload, String tag);

    /**
     * 发送消息给拥有指定标签的用户
     *
     * @param cmd     路由命令
     * @param payload 消息内容
     * @param tags    标签集合
     */
    void sendMessageByTags(String cmd, Object payload, Collection<String> tags);

    /**
     * 发送消息给拥有任意指定标签的用户
     *
     * @param cmd     路由命令
     * @param payload 消息内容
     * @param tags    标签集合
     */
    void sendMessageByAnyTags(String cmd, Object payload, Collection<String> tags);

    /**
     * 发送消息给客户端
     *
     * @param message 消息
     */
    void sendMessage(Message message);

    /**
     * 发送消息给客户端
     *
     * @param messages 消息列表
     */
    void sendMessages(Collection<Message> messages);

    // endregion
}
