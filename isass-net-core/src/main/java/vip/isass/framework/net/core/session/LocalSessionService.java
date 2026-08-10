// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.core.session;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.cglib.CglibUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Configuration;
import vip.isass.framework.common.map.MultiKeyMultiValueBiMap;
import vip.isass.framework.common.map.MultiValueBiMap;
import vip.isass.framework.net.core.message.Message;

import jakarta.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 会话管理器抽象类
 *
 * @author Rain
 */
@Slf4j
@Configuration
@ConditionalOnMissingBean(name = "sessionServiceClientProxy")
public class LocalSessionService implements ISessionService {

    // region sessionId 和 session 关系

    /**
     * 保存所有会话
     * <p> {@literal Map<sessionId, Session>}
     */
    private final Map<String, Session<?>> sessionMap = new ConcurrentHashMap<>();

    /**
     * 所有会话 map 的不可变 map
     */
    private final Map<String, Session<?>> unmodifiableSessionMap = Collections.unmodifiableMap(sessionMap);

    // endregion

    private final MultiValueBiMap<String, String> userAndSessionMap = new MultiValueBiMap<>();

    private final MultiValueBiMap<String, String> aliasAndSessionMap = new MultiValueBiMap<>();

    private final MultiKeyMultiValueBiMap<String, String> sessionAndTagMap = new MultiKeyMultiValueBiMap<>();

    // region session

    @Override
    public void addSession(Session<?> session) {
        Assert.notNull(session, "session 不能为 null");
        sessionMap.put(session.getSessionId(), session);
    }

    @Override
    public Session<?> removeSession(String sessionId) {
        Session<?> remove = sessionMap.remove(sessionId);
        if (remove != null) {
            removeUserId(sessionId);
            removeAlias(sessionId);
            removeTags(sessionId);
        }
        return remove;
    }

    @Override
    public Session<?> getSessionById(String sessionId) {
        return sessionMap.get(sessionId);
    }

    // @Override
    // public Collection<String> findSessionIds(String userId) {
    //     return userAndSessionMap.get(userId);
    // }

    @Override
    public Collection<Session<?>> findAllSessions() {
        return unmodifiableSessionMap.values();
    }

    @Override
    public SessionInfoCollection getSessionInfoCollection() {
        return SessionInfoCollection.builder()
                .sessions(sessionMap.values()
                        .parallelStream()
                        .map(s -> CglibUtil.copy(s, DisplaySession.class))
                        .collect(Collectors.toList()))
                .userAndSessionMap(userAndSessionMap)
                .aliasAndSessionMap(aliasAndSessionMap)
                .sessionAndTagMap(sessionAndTagMap)
                .build();
    }

    // endregion

    // region user

    @Override
    public String getUserId(String sessionId) {
        return userAndSessionMap.getKey(sessionId);
    }

    @Override
    public void setUserId(String sessionId, String userId) {
        Session<?> session = sessionMap.get(sessionId);
        if (session == null) {
            return;
        }
        userAndSessionMap.removeValue(sessionId);
        userAndSessionMap.put(userId, sessionId);
    }

    @Override
    public void removeUserId(String sessionId) {
        userAndSessionMap.removeValue(sessionId);
    }

    @Override
    public Map<String, Boolean> isOnline(Collection<String> userIds) {
        Map<String, Boolean> result = MapUtil.newHashMap(userIds.size());
        for (String userId : userIds) {
            result.put(userId, CollUtil.isNotEmpty(userAndSessionMap.get(userId)));
        }
        return result;
    }

    // endregion

    // region alias

    @Override
    public String getAlias(String sessionId) {
        return aliasAndSessionMap.getKey(sessionId);
    }

    @Override
    public Collection<String> findSessionIdsByAlias(String alias) {
        return aliasAndSessionMap.get(alias);
    }

    @Override
    public Collection<String> findAliases(String prefix) {
        if (StrUtil.isBlank(prefix)) {
            return aliasAndSessionMap.keys();
        }

        return aliasAndSessionMap.keys()
                .stream()
                .filter(k -> k.startsWith(prefix))
                .collect(Collectors.toList());
    }

    @Override
    public void setAlias(String sessionId, String alias) {
        Session<?> session = sessionMap.get(sessionId);
        if (session == null) {
            return;
        }
        aliasAndSessionMap.removeValue(sessionId);
        aliasAndSessionMap.put(alias, sessionId);
    }

    @Override
    public void removeAlias(String sessionId) {
        aliasAndSessionMap.removeValue(sessionId);
    }

    // endregion

    // region tag

    @Override
    public Collection<String> findTags(String sessionId) {
        return sessionAndTagMap.get(sessionId);
    }

    @Override
    public Collection<String> findTagsByUserId(String userId) {
        Collection<String> sessionIds = userAndSessionMap.get(userId);
        if (CollUtil.isEmpty(sessionIds)) {
            return Collections.emptySet();
        }
        return sessionIds.stream()
                .map(this::findTags)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    // @Override
    // public Collection<String> findSessionIds(Collection<String> tags) {
    //     Set<String> sessionIds = null;
    //     for (String tag : tags) {
    //         Collection<String> tempSessionIds = sessionAndTagMap.getKey(tag);
    //         if (CollUtil.isEmpty(tempSessionIds)) {
    //             return Collections.emptySet();
    //         }
    //
    //         if (sessionIds == null) {
    //             sessionIds = new HashSet<>(tempSessionIds);
    //             continue;
    //         }
    //
    //         sessionIds.retainAll(tempSessionIds);
    //         if (sessionIds.isEmpty()) {
    //             return Collections.emptySet();
    //         }
    //     }
    //     return sessionIds;
    // }

    @Override
    public Collection<String> findSessionsByAnyMatchTags(Collection<String> tags) {
        return tags.stream()
                .map(sessionAndTagMap::getKey)
                .filter(CollUtil::isNotEmpty)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean containAnyTag(@Nonnull String sessionId, @Nonnull Collection<String> tags) {
        Collection<String> existingTags = sessionAndTagMap.get(sessionId);
        return CollUtil.containsAny(existingTags, tags);
    }

    @Override
    public boolean containAllTags(String sessionId, Collection<String> tags) {
        Collection<String> existingTags = sessionAndTagMap.get(sessionId);
        return CollUtil.containsAll(existingTags, tags);
    }

    @Override
    public void setTags(String sessionId, Collection<String> tags) {
        Session<?> session = sessionMap.get(sessionId);
        if (session == null) {
            return;
        }
        sessionAndTagMap.replaceValues(sessionId, tags);
    }

    @Override
    public void addTags(String sessionId, Collection<String> tags) {
        Session<?> session = sessionMap.get(sessionId);
        if (session == null) {
            return;
        }
        sessionAndTagMap.putAll(sessionId, tags);
    }

    @Override
    public void setTagsByUserId(String userId, Collection<String> tags) {
        Collection<String> sessionIds = userAndSessionMap.get(userId);
        if (CollUtil.isEmpty(sessionIds)) {
            return;
        }
        sessionIds.forEach(s -> setTags(s, tags));
    }

    @Override
    public void addTagsByUserId(String userId, Collection<String> tags) {
        Collection<String> sessionIds = userAndSessionMap.get(userId);
        if (CollUtil.isEmpty(sessionIds)) {
            return;
        }
        sessionIds.forEach(s -> addTags(s, tags));
    }

    @Override
    public void removeTags(String sessionId) {
        sessionAndTagMap.removeAll(sessionId);
    }

    @Override
    public void removeTags(String sessionId, Collection<String> tags) {
        Session<?> session = sessionMap.get(sessionId);
        if (session == null) {
            return;
        }
        sessionAndTagMap.removeValues(sessionId, tags);
    }

    @Override
    public void removeTagsByUserId(String userId, Collection<String> tags) {
        Collection<String> sessionIds = userAndSessionMap.get(userId);
        if (CollUtil.isEmpty(sessionIds)) {
            return;
        }
        sessionIds.forEach(s -> removeTags(s, tags));
    }

    // endregion

    // region message

    @Override
    public void broadcastMessage(String cmd, Object payload) {
        sessionMap.entrySet()
                .parallelStream()
                .forEach(entry -> entry.getValue().sendMessage(cmd, payload));
    }

    @Override
    public void sendMessageByUserId(String cmd, Object payload, String userId) {
        Collection<String> sessions = userAndSessionMap.get(userId);
        if (CollUtil.isEmpty(sessions)) {
            return;
        }
        sessions.parallelStream()
                .map(sessionMap::get)
                .forEach(s -> s.sendMessage(cmd, payload));
    }

    @Override
    public void sendMessageByUserIds(String cmd, Object payload, Collection<String> userIds) {
        for (String userId : userIds) {
            sendMessageByUserId(cmd, payload, userId);
        }
    }

    @Override
    public void sendMessageToLoginUsers(String cmd, Object payload) {
        userAndSessionMap.entries()
                .parallelStream()
                .map(Map.Entry::getValue)
                .map(sessionMap::get)
                .forEach(s -> s.sendMessage(cmd, payload));
    }

    @Override
    public void sendMessageByAlias(String cmd, Object payload, String alias) {
        Collection<String> sessionIds = aliasAndSessionMap.get(alias);
        if (sessionIds == null) {
            return;
        }
        sessionIds.parallelStream()
                .map(sessionMap::get)
                .forEach(s -> s.sendMessage(cmd, payload));
    }

    @Override
    public void sendMessageByAlias(String cmd, Object payload, Collection<String> aliases) {
        Set<String> sentSessionIds = new HashSet<>();
        for (String alias : aliases) {
            Collection<String> sessionIds = aliasAndSessionMap.get(alias);
            if (sessionIds == null) {
                continue;
            }
            for (String sessionId : sessionIds) {
                if (sentSessionIds.contains(sessionId)) {
                    continue;
                }
                sentSessionIds.add(sessionId);
                Session<?> session = sessionMap.get(sessionId);
                if (session == null) {
                    continue;
                }
                session.sendMessage(cmd, payload);
            }
        }
    }

    @Override
    public void sendMessageByTag(String cmd, Object payload, String tag) {
        Collection<String> sessionIds = sessionAndTagMap.getKey(tag);
        if (sessionIds == null) {
            return;
        }
        sessionIds.parallelStream()
                .map(sessionMap::get)
                .forEach(s -> s.sendMessage(cmd, payload));
    }

    @Override
    public void sendMessageByTags(String cmd, Object payload, Collection<String> tags) {
        Set<String> sentSessionIds = new HashSet<>();
        for (String tag : tags) {
            Collection<String> sessionIds = sessionAndTagMap.getKey(tag);
            if (CollUtil.isEmpty(sessionIds)) {
                continue;
            }

            for (String sessionId : sessionIds) {
                if (sentSessionIds.contains(sessionId)) {
                    continue;
                }
                sentSessionIds.add(sessionId);
                Session<?> session = sessionMap.get(sessionId);
                if (session == null) {
                    continue;
                }
                session.sendMessage(cmd, payload);
            }
        }
    }

    @Override
    public void sendMessageByAnyTags(String cmd, Object payload, Collection<String> tags) {
        Map<String, Boolean> sentSessionIds = new ConcurrentHashMap<>();
        for (String tag : tags) {
            Collection<String> sessionIds = sessionAndTagMap.getKey(tag);
            if (sessionIds == null) {
                continue;
            }

            sessionIds.parallelStream()
                    .filter(s -> sentSessionIds.putIfAbsent(s, Boolean.TRUE) == null)
                    .map(sessionMap::get)
                    .forEach(s -> s.sendMessage(cmd, payload));
        }
    }

    /**
     * 发送消息
     * todo session 对象添加发送二进制消息的方法，避免循环发送消息时多次重复的消息序列化
     * 因发送消息是高频调用接口，里面又有大量集合的判断，为避免集合不必要的复制，所以逻辑比较冗长
     *
     * @param message 消息
     */
    @Override
    public void sendMessage(Message message) {
        // 1：判断 receiverSession 和 receiverSessionId
        if (message.getReceiverSession() != null) {
            message.getReceiverSession().sendMessage(message.getCmd(), message.getPayload());
            return;
        }
        if (StrUtil.isNotBlank(message.getReceiverSessionId())) {
            Session<?> session = sessionMap.get(message.getReceiverSessionId());
            if (session == null) {
                return;
            }
            session.sendMessage(message.getCmd(), message.getPayload());
            return;
        }

        // 2：判断 userId
        Set<String> finalSessionIds = null;
        boolean modifiable = false; // finalSessionIds 是否可修改的集合
        if (CollUtil.isNotEmpty(message.getUserIds())) {
            for (String userId : message.getUserIds()) {
                Collection<String> sessionIdsFromUserId = userAndSessionMap.get(userId);
                if (CollUtil.isEmpty(sessionIdsFromUserId)) {
                    continue;
                }
                if (finalSessionIds == null) {
                    if (sessionIdsFromUserId instanceof Set) {
                        finalSessionIds = (Set<String>) sessionIdsFromUserId;
                    } else {
                        finalSessionIds = new HashSet<>(sessionIdsFromUserId);
                        modifiable = true;
                    }
                } else {
                    if (modifiable) {
                        finalSessionIds.addAll(sessionIdsFromUserId);
                    } else {
                        finalSessionIds = new HashSet<>(finalSessionIds);
                        modifiable = true;
                    }
                }
            }

            if (finalSessionIds == null) {
                return;
            }
        }

        // 3：叠加判断 alias
        if (CollUtil.isNotEmpty(message.getAliases())) {
            for (String alias : message.getAliases()) {
                Collection<String> sessionIdsFromAlias = aliasAndSessionMap.get(alias);
                if (CollUtil.isEmpty(sessionIdsFromAlias)) {
                    continue;
                }

                // 如果 userId 和 alias 都设置了，则判断其交集
                if (finalSessionIds == null) {
                    if (sessionIdsFromAlias instanceof Set) {
                        finalSessionIds = (Set<String>) sessionIdsFromAlias;
                    } else {
                        finalSessionIds = new HashSet<>(sessionIdsFromAlias);
                        modifiable = true;
                    }
                } else {
                    if (!modifiable) {
                        finalSessionIds = new HashSet<>(finalSessionIds);
                        modifiable = true;
                    }
                    finalSessionIds.retainAll(sessionIdsFromAlias);
                    if (finalSessionIds.isEmpty()) {
                        return;
                    }
                }
            }

            if (CollUtil.isEmpty(finalSessionIds)) {
                return;
            }
        }

        // 4：叠加判断 tags
        if (CollUtil.isNotEmpty(message.getTags())) {
            for (String tag : message.getTags()) {
                Collection<String> sessionIdsFromKey = sessionAndTagMap.getKey(tag);
                if (CollUtil.isEmpty(sessionIdsFromKey)) {
                    return;
                }

                // 如果上一步找到了sessionId，则判断此步找到的sessionId是否被上一步的包含
                if (finalSessionIds == null) {
                    if (sessionIdsFromKey instanceof Set) {
                        finalSessionIds = (Set<String>) sessionIdsFromKey;
                    } else {
                        finalSessionIds = new HashSet<>(sessionIdsFromKey);
                        modifiable = true;
                    }
                } else {
                    if (!modifiable) {
                        finalSessionIds = new HashSet<>(finalSessionIds);
                        modifiable = true;
                    }
                    finalSessionIds.retainAll(sessionIdsFromKey);
                    if (finalSessionIds.isEmpty()) {
                        return;
                    }
                }
            }

            // 因为设置了 tags，所以忽略判断 tagsAny，如果 finalSessionIds 非空，则给这些会话发送消息
            if (!finalSessionIds.isEmpty()) {
                finalSessionIds.parallelStream()
                        .map(sessionMap::get)
                        .filter(Objects::nonNull)
                        .forEach(s -> s.sendMessage(message.getCmd(), message.getPayload()));
            }
            return;
        }

        // 5：叠加判断 tagsAny
        if (CollUtil.isNotEmpty(message.getTagsAny())) {
            Map<String, Boolean> sentSessionIds = new ConcurrentHashMap<>();
            for (String tag : message.getTagsAny()) {
                Collection<String> sessionIdsFromKey = sessionAndTagMap.getKey(tag);
                if (CollUtil.isEmpty(sessionIdsFromKey)) {
                    continue;
                }

                if (finalSessionIds.isEmpty()) {
                    sessionIdsFromKey.parallelStream()
                            .filter(s -> sentSessionIds.putIfAbsent(s, Boolean.TRUE) == null)
                            .map(sessionMap::get)
                            .filter(Objects::nonNull)
                            .forEach(s -> s.sendMessage(message.getCmd(), message.getPayload()));
                } else {
                    Collection<String> firstColl;
                    Collection<String> secondColl;
                    if (sessionIdsFromKey.size() > finalSessionIds.size()) {
                        firstColl = finalSessionIds;
                        secondColl = sessionIdsFromKey;
                    } else {
                        firstColl = sessionIdsFromKey;
                        secondColl = finalSessionIds;
                    }
                    for (String loopSessionId : firstColl) {
                        if (secondColl.contains(loopSessionId)
                                && sentSessionIds.putIfAbsent(loopSessionId, Boolean.TRUE) == null) {
                            Session<?> session = sessionMap.get(loopSessionId);
                            if (session != null) {
                                session.sendMessage(message.getCmd(), message.getPayload());
                            }
                        }
                    }
                }
            }
            return;
        }

        // 6：最后方法依然没 return，则广播或者发消息给 finalSessionIds
        if (finalSessionIds == null) {
            broadcastMessage(message.getCmd(), message.getPayload());
        } else {
            finalSessionIds.parallelStream()
                    .map(sessionMap::get)
                    .filter(Objects::nonNull)
                    .forEach(s -> s.sendMessage(message.getCmd(), message.getPayload()));
        }
    }

    @Override
    public void sendMessages(Collection<Message> messages) {
        messages.parallelStream().forEach(this::sendMessage);
    }

    // endregion

}