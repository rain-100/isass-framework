// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.proxy.upstream;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import tools.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import vip.isass.framework.common.support.JsonUtil;
import vip.isass.framework.common.support.BeanProviderUtil;
import vip.isass.framework.common.support.okhttp.OkHttpUtil;
import vip.isass.framework.common.web.Resp;
import vip.isass.framework.net.core.NetRedisKey;
import vip.isass.framework.net.core.message.Message;
import vip.isass.framework.net.core.server.NetProtocol;
import vip.isass.framework.net.core.server.NetServerInfo;
import vip.isass.framework.net.core.server.allocator.INodeAllocatorService;
import vip.isass.framework.net.core.session.ISessionService;
import vip.isass.framework.net.core.session.Session;
import vip.isass.framework.net.core.session.SessionBindingInfoChangeReq;
import vip.isass.framework.net.core.session.SessionInfoCollection;

import jakarta.annotation.Nonnull;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
public class SessionServiceClientProxy implements ISessionService {

    public static final OkHttpClient CLIENT;

    private static final TypeReference<Resp<String>> STRING_RESP_TYPE_REF = new TypeReference<Resp<String>>() {
    };

    private static final TypeReference<Resp<Collection<String>>> COLL_STRING_RESP_TYPE_REF = new TypeReference<Resp<Collection<String>>>() {
    };

    private static final TypeReference<Resp<Boolean>> BOOLEAN_RESP_TYPE_REF = new TypeReference<Resp<Boolean>>() {
    };

    private static final TypeReference<Resp<Map<String, Boolean>>> MAP_STRING_BOOLEAN_RESP_TYPE_REF = new TypeReference<Resp<Map<String, Boolean>>>() {
    };

    private static final TypeReference<Resp<SessionInfoCollection>> SESSION_INFO_COLLECTION_RESP_TYPE_REF = new TypeReference<Resp<SessionInfoCollection>>() {
    };

    static {
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(100);
        dispatcher.setMaxRequestsPerHost(100);
        CLIENT = new OkHttpClient.Builder()
                .dispatcher(dispatcher)
                .connectionPool(new ConnectionPool(10, 10, TimeUnit.MINUTES))
                .build();
    }

    private final String defaultProtocol;

    private NetProtocol defaultNetProtocol;

    private final RedisTemplate<String, Object> redisTemplate;

    private Map<NetProtocol, INodeAllocatorService> nodeAllocatorServiceMap;

    public SessionServiceClientProxy(@Value("${kernel.net.defaultProtocol:}") String defaultProtocol,
                                     RedisTemplate<String, Object> redisTemplate) {
        this.defaultProtocol = defaultProtocol;
        this.redisTemplate = redisTemplate;
    }

    @Scheduled(fixedDelay = 10 * 1000)
    private void reloadNodeAllocatorService() {
        Collection<INodeAllocatorService> nodeAllocatorServices = BeanProviderUtil.getBeans(INodeAllocatorService.class);
        if (CollUtil.isEmpty(nodeAllocatorServices)) {
            this.nodeAllocatorServiceMap = Collections.emptyMap();
            return;
        }

        this.nodeAllocatorServiceMap = nodeAllocatorServices.stream()
                .collect(Collectors.toMap(INodeAllocatorService::getNetProtocol, Function.identity()));

        if (StrUtil.isBlank(defaultProtocol)) {
            defaultNetProtocol = nodeAllocatorServiceMap.values().iterator().next().getNetProtocol();
        } else {
            try {
                defaultNetProtocol = NetProtocol.valueOf(defaultProtocol);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("kernel.net.defaultProtocol 配置错误，请检查");
            }
        }
    }

    @Override
    public void addSession(Session<?> session) {
        throw new UnsupportedOperationException("net proxy client cannot add session");
    }

    @Override
    public Session<?> removeSession(String sessionId) {
        throw new UnsupportedOperationException("net proxy client cannot remove session");
    }

    @Override
    public Session<?> getSessionById(String sessionId) {
        throw new UnsupportedOperationException("net proxy client cannot get session");
    }

    // @Override
    // public Collection<String> findSessionIds(String userId) {
    //     INodeAllocatorService nodeAllocatorService = nodeAllocatorServiceMap.get(defaultNetProtocol);
    //     NetServerInfo info = nodeAllocatorService.allocate(null);
    //     String url = StrUtil.format(
    //             "http{}://{}:{}/{}/session",
    //             info.getHttpSecure() ? "s" : "",
    //             info.getInternalIp(),
    //             info.getHttpPort(),
    //             info.getNetProtocol().getServiceName()
    //     );
    //     Resp<Collection<String>> resp = OkHttpUtil.get(
    //             url,
    //             null,
    //             MapUtil.<String, String>builder().put("userId", userId).build(),
    //             COLL_STRING_RESP_TYPE_REF);
    //     return resp.dataIfSuccessOrException();
    // }

    @Override
    public Collection<Session<?>> findAllSessions() {
        throw new UnsupportedOperationException("net proxy client cannot get session");
    }

    @Override
    public SessionInfoCollection getSessionInfoCollection() {
        return fetchGetFromAllNode(
                StrUtil.format("/net/admin/session/sessionInfoCollection"),
                null,
                Objects::nonNull,
                SESSION_INFO_COLLECTION_RESP_TYPE_REF);
    }

    @Override
    public String getUserId(String sessionId) {
        return fetchGetFromAllNode(
                StrUtil.format("/session/{}/userId", sessionId),
                null,
                StrUtil::isNotBlank,
                STRING_RESP_TYPE_REF);
    }

    @Override
    public void setUserId(String sessionId, String userId) {
        saveSessionInfo(SessionBindingInfoChangeReq.builder()
                .sessionId(sessionId)
                .resetUserId(userId)
                .build());
    }

    @Override
    public Map<String, Boolean> isOnline(Collection<String> userIds) {
        return fetchPostFromAllNode(
                "/session/user/isOnline",
                null,
                userIds,
                (httpReturnMap, methodRetuenMap) -> {
                    if (MapUtil.isEmpty(httpReturnMap)) {
                        return methodRetuenMap;
                    }

                    if (MapUtil.isEmpty(methodRetuenMap)) {
                        return httpReturnMap;
                    }

                    for (Map.Entry<String, Boolean> entry : httpReturnMap.entrySet()) {
                        Boolean b = methodRetuenMap.get(entry.getKey());
                        if (b == null) {
                            methodRetuenMap.put(entry.getKey(), entry.getValue());
                            continue;
                        }
                        methodRetuenMap.merge(entry.getKey(), entry.getValue(), (oldValue, newValue) -> oldValue || newValue);
                    }
                    return methodRetuenMap;
                },
                MAP_STRING_BOOLEAN_RESP_TYPE_REF);
    }

    @Override
    public void removeUserId(String sessionId) {
        saveSessionInfo(SessionBindingInfoChangeReq.builder()
                .sessionId(sessionId)
                .removeUserId(Boolean.TRUE)
                .build());
    }

    @Override
    public String getAlias(String sessionId) {
        return fetchGetFromAllNode(
                StrUtil.format("/session/{}/alias", sessionId),
                null,
                StrUtil::isNotBlank,
                STRING_RESP_TYPE_REF);
    }

    @Override
    public Collection<String> findSessionIdsByAlias(String alias) {
        return fetchGetFromAllNode(
                "/session/sessionIds",
                MapUtil.<String, Object>builder().put("alias", alias).build(),
                CollUtil::isNotEmpty,
                COLL_STRING_RESP_TYPE_REF);
    }

    @Override
    public Collection<String> findAliases(String prefix) {
        return fetchGetFromAllNode(
                "/session/alias",
                MapUtil.<String, Object>builder().put("prefix", prefix).build(),
                CollUtil::isNotEmpty,
                COLL_STRING_RESP_TYPE_REF);
    }

    @Override
    public void setAlias(String sessionId, String alias) {
        saveSessionInfo(SessionBindingInfoChangeReq.builder()
                .sessionId(sessionId)
                .alias(alias)
                .build());
    }

    @Override
    public void removeAlias(String sessionId) {
        saveSessionInfo(SessionBindingInfoChangeReq.builder()
                .sessionId(sessionId)
                .removeAlias(Boolean.TRUE)
                .build());
    }

    @Override
    public Collection<String> findTags(String sessionId) {
        return fetchGetFromAllNode(
                StrUtil.format("/session/{}/tags", sessionId),
                null,
                CollUtil::isNotEmpty,
                COLL_STRING_RESP_TYPE_REF);
    }

    @Override
    public Collection<String> findTagsByUserId(String userId) {
        return fetchGetFromAllNode(
                StrUtil.format("/session/tags/{}", userId),
                null,
                CollUtil::isNotEmpty,
                COLL_STRING_RESP_TYPE_REF);
    }

    // @Override
    // public Collection<String> findSessionIds(Collection<String> tags) {
    //     INodeAllocatorService nodeAllocatorService = nodeAllocatorServiceMap.get(defaultNetProtocol);
    //     NetServerInfo info = nodeAllocatorService.allocate(null);
    //     String url = StrUtil.format(
    //             "http{}://{}:{}/{}/session/",
    //             info.getHttpSecure() ? "s" : "",
    //             info.getInternalIp(),
    //             info.getHttpPort(),
    //             info.getNetProtocol().getServiceName()
    //     );
    //     Resp<Collection<String>> resp = OkHttpUtil.get(
    //             url,
    //             null,
    //             MapUtil.<String, Collection<String>>builder()
    //                     .put("tags", tags)
    //                     .build(),
    //             COLL_STRING_RESP_TYPE_REF);
    //     return resp.dataIfSuccessOrException();
    // }

    @Override
    public Collection<String> findSessionsByAnyMatchTags(Collection<String> tags) {
        return fetchGetFromAllNode(
                "/session/any",
                MapUtil.<String, Object>builder()
                        .put("tags", tags)
                        .build(),
                CollUtil::isNotEmpty,
                COLL_STRING_RESP_TYPE_REF);
    }

    @Override
    public boolean containAnyTag(@Nonnull String sessionId, @Nonnull Collection<String> tags) {
        return ObjectUtil.defaultIfNull(
                fetchGetFromAllNode(
                        StrUtil.format("/session/{}/containAnyTag", sessionId),
                        MapUtil.<String, Object>builder()
                                .put("tags", tags)
                                .build(),
                        Boolean.TRUE::equals,
                        BOOLEAN_RESP_TYPE_REF),
                Boolean.FALSE);
    }

    @Override
    public boolean containAllTags(String sessionId, Collection<String> tags) {
        return ObjectUtil.defaultIfNull(
                fetchGetFromAllNode(
                        StrUtil.format("/session/{}/containTags", sessionId),
                        MapUtil.<String, Object>builder()
                                .put("tags", tags)
                                .build(),
                        Boolean.TRUE::equals,
                        BOOLEAN_RESP_TYPE_REF),
                Boolean.FALSE);
    }

    @Override
    public void setTags(String sessionId, Collection<String> tags) {
        saveSessionInfo(SessionBindingInfoChangeReq.builder()
                .sessionId(sessionId)
                .tags(tags)
                .build());
    }

    @Override
    public void setTagsByUserId(String userId, Collection<String> tags) {
        saveSessionInfo(SessionBindingInfoChangeReq.builder()
                .userId(userId)
                .tags(tags)
                .build());
    }

    @Override
    public void addTags(String sessionId, Collection<String> tags) {
        saveSessionInfo(SessionBindingInfoChangeReq.builder()
                .sessionId(sessionId)
                .addTags(tags)
                .build());
    }

    @Override
    public void addTagsByUserId(String userId, Collection<String> tags) {
        saveSessionInfo(SessionBindingInfoChangeReq.builder()
                .userId(userId)
                .addTags(tags)
                .build());
    }

    @Override
    public void removeTags(String sessionId) {
        saveSessionInfo(SessionBindingInfoChangeReq.builder()
                .sessionId(sessionId)
                .removeAllTags(Boolean.TRUE)
                .build());
    }

    @Override
    public void removeTags(String sessionId, Collection<String> tags) {
        saveSessionInfo(SessionBindingInfoChangeReq.builder()
                .sessionId(sessionId)
                .removeTags(tags)
                .build());
    }

    @Override
    public void removeTagsByUserId(String userId, Collection<String> tags) {
        saveSessionInfo(SessionBindingInfoChangeReq.builder()
                .userId(userId)
                .removeTags(tags)
                .build());
    }

    @Override
    public void broadcastMessage(String cmd, Object payload) {
        redisTemplate.convertAndSend(
                NetRedisKey.REDIS_PUBSUB_KEY,
                Message.builder()
                        .cmd(cmd)
                        .payload(payload)
                        .build());
    }

    @Override
    public void sendMessageByUserId(String cmd, Object payload, String userId) {
        redisTemplate.convertAndSend(
                NetRedisKey.REDIS_PUBSUB_KEY,
                Message.builder()
                        .cmd(cmd)
                        .payload(payload)
                        .userIds(Collections.singleton(userId))
                        .build());
    }

    @Override
    public void sendMessageByUserIds(String cmd, Object payload, Collection<String> userIds) {
        redisTemplate.convertAndSend(
                NetRedisKey.REDIS_PUBSUB_KEY,
                Message.builder()
                        .cmd(cmd)
                        .payload(payload)
                        .userIds(userIds)
                        .build());
    }

    @Override
    public void sendMessageToLoginUsers(String cmd, Object payload) {
        redisTemplate.convertAndSend(
                NetRedisKey.REDIS_PUBSUB_KEY,
                Message.builder()
                        .cmd(cmd)
                        .payload(payload)
                        .userIds(Collections.singleton("LoginUser"))
                        .build());
    }

    @Override
    public void sendMessageByAlias(String cmd, Object payload, String alias) {
        redisTemplate.convertAndSend(
                NetRedisKey.REDIS_PUBSUB_KEY,
                Message.builder()
                        .cmd(cmd)
                        .payload(payload)
                        .aliases(Collections.singleton(alias))
                        .build());
    }

    @Override
    public void sendMessageByAlias(String cmd, Object payload, Collection<String> aliases) {
        redisTemplate.convertAndSend(
                NetRedisKey.REDIS_PUBSUB_KEY,
                Message.builder()
                        .cmd(cmd)
                        .payload(payload)
                        .aliases(aliases)
                        .build());
    }

    @Override
    public void sendMessageByTag(String cmd, Object payload, String tag) {
        redisTemplate.convertAndSend(
                NetRedisKey.REDIS_PUBSUB_KEY,
                Message.builder()
                        .cmd(cmd)
                        .payload(payload)
                        .tags(Collections.singleton(tag))
                        .build());
    }

    @Override
    public void sendMessageByTags(String cmd, Object payload, Collection<String> tags) {
        redisTemplate.convertAndSend(
                NetRedisKey.REDIS_PUBSUB_KEY,
                Message.builder()
                        .cmd(cmd)
                        .payload(payload)
                        .tags(tags)
                        .build());
    }

    @Override
    public void sendMessageByAnyTags(String cmd, Object payload, Collection<String> tags) {
        redisTemplate.convertAndSend(
                NetRedisKey.REDIS_PUBSUB_KEY,
                Message.builder()
                        .cmd(cmd)
                        .payload(payload)
                        .tagsAny(tags)
                        .build());
    }

    @Override
    public void sendMessage(Message message) {
        redisTemplate.convertAndSend(NetRedisKey.REDIS_PUBSUB_KEY, message);
    }

    @Override
    public void sendMessages(Collection<Message> messages) {
        for (Message message : messages) {
            redisTemplate.convertAndSend(NetRedisKey.REDIS_PUBSUB_KEY, message);
        }
    }

    /**
     * 从所有网关节点获取会话信息
     *
     * @param urlSuffix     url 后缀，用于拼接完整的服务地址
     * @param requestParam  请求参数
     * @param checkHttpResp 网关节点响应的数据是否符合要求需要返回给调用方
     * @param typeReference 用于网关节点返回数据时用到的反序列化
     * @param <T>           返回给调用方的对象类型
     * @return 数据
     */
    @SuppressWarnings("unchecked")
    private <T> T fetchGetFromAllNode(String urlSuffix,
                                      Map<String, Object> requestParam,
                                      Predicate<T> checkHttpResp,
                                      TypeReference<Resp<T>> typeReference) {
        INodeAllocatorService nodeAllocatorService = nodeAllocatorServiceMap.get(defaultNetProtocol);
        Collection<NetServerInfo> serverInfos = nodeAllocatorService.getAll();
        Assert.notEmpty(serverInfos, "未发现[{}]网关，执行Get[{}]失败", defaultNetProtocol, urlSuffix);
        Object[] returnArr = new Object[1];
        CompletableFuture<T>[] futures = (CompletableFuture<T>[]) new CompletableFuture<?>[serverInfos.size()];
        String[] urls = new String[serverInfos.size()];
        int idx = 0;
        for (NetServerInfo serverInfo : serverInfos) {
            urls[idx] = StrUtil.format(
                    "http{}://{}:{}/{}" + urlSuffix,
                    serverInfo.getHttpSecure() ? "s" : "",
                    serverInfo.getInternalIp(),
                    serverInfo.getHttpPort(),
                    serverInfo.getNetProtocol().getServiceName()
            );
            String url = urls[idx];
            futures[idx] = CompletableFuture.supplyAsync(() -> OkHttpUtil.get(url, null, requestParam, typeReference)
                            .dataIfSuccessOrException())
                    .whenComplete((returnData, throwable) -> {
                        if (checkHttpResp.test(returnData)) {
                            returnArr[0] = returnData;
                        }
                    });
            idx++;
        }

        CompletableFuture<Void> voidCompletableFuture = CompletableFuture.allOf(futures);
        try {
            voidCompletableFuture.get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("请求网关[{}]获取信息错误", defaultNetProtocol, e);
            log.error("获取到的网关url：[{}]", CollUtil.join(Arrays.asList(urls), ","));
        }
        return (T) returnArr[0];
    }

    @SuppressWarnings("unchecked")
    private <T> T fetchPostFromAllNode(String urlSuffix,
                                       Map<String, Object> requestParam,
                                       Object requestBody,
                                       BiFunction<T, T, T> mapReduce,
                                       TypeReference<Resp<T>> typeReference) {
        INodeAllocatorService nodeAllocatorService = nodeAllocatorServiceMap.get(defaultNetProtocol);
        Collection<NetServerInfo> serverInfos = nodeAllocatorService.getAll();
        Assert.notEmpty(serverInfos, "未发现[{}]网关，根据会话id获取用户id失败", defaultNetProtocol);
        Object[] returnArr = new Object[1];
        CompletableFuture<T>[] futures = (CompletableFuture<T>[]) new CompletableFuture<?>[serverInfos.size()];
        int idx = 0;
        for (NetServerInfo serverInfo : serverInfos) {
            futures[idx] = CompletableFuture.supplyAsync(() -> {
                        String url = StrUtil.format(
                                "http{}://{}:{}/{}" + urlSuffix,
                                serverInfo.getHttpSecure() ? "s" : "",
                                serverInfo.getInternalIp(),
                                serverInfo.getHttpPort(),
                                serverInfo.getNetProtocol().getServiceName()
                        );
                        return OkHttpUtil.post(url, null, requestParam, requestBody, typeReference)
                                .dataIfSuccessOrException();
                    })
                    .whenComplete((returnData, throwable) -> {
                        returnArr[0] = mapReduce.apply(returnData, (T) returnArr[0]);
                    });
            idx++;
        }

        CompletableFuture<Void> voidCompletableFuture = CompletableFuture.allOf(futures);
        try {
            voidCompletableFuture.get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("请求网关[{}]获取信息错误", defaultNetProtocol, e);
        }
        return (T) returnArr[0];
    }

    @SuppressWarnings("unchecked")
    private void saveSessionInfo(SessionBindingInfoChangeReq req) {
        INodeAllocatorService nodeAllocatorService = nodeAllocatorServiceMap.get(defaultNetProtocol);
        Collection<NetServerInfo> serverInfos = nodeAllocatorService.getAll();
        Assert.notEmpty(serverInfos, "未发现[{}]网关，根据会话id获取用户id失败", defaultNetProtocol);
        final okhttp3.RequestBody requestBody = okhttp3.RequestBody.create(
                okhttp3.MediaType.get(MediaType.APPLICATION_JSON_VALUE),
                JsonUtil.writeValue(req));
        CompletableFuture<Void>[] futures = (CompletableFuture<Void>[]) new CompletableFuture<?>[serverInfos.size()];
        int idx = 0;
        for (NetServerInfo serverInfo : serverInfos) {
            futures[idx] = CompletableFuture.runAsync(() -> {
                String url = StrUtil.format(
                        "http{}://{}:{}/{}/session/info",
                        serverInfo.getHttpSecure() ? "s" : "",
                        serverInfo.getInternalIp(),
                        serverInfo.getHttpPort(),
                        serverInfo.getNetProtocol().getServiceName()
                );

                Request request = new Request.Builder()
                        .url(url)
                        .post(requestBody)
                        .build();
                Call call = CLIENT.newCall(request);
                try (Response execute = call.execute();
                     ResponseBody body = execute.body();) {
                    String bodyStr = body == null ? "" : body.string();
                    if (bodyStr.contains("false")) {
                        JsonUtil.readValue(bodyStr, BOOLEAN_RESP_TYPE_REF).exceptionIfUnSuccess();
                    } else if (!execute.isSuccessful()) {
                        String msg = StrUtil.format(
                                "调用[post {}]失败，状态码：{},响应体：{}",
                                url,
                                execute.code(),
                                body == null ? "" : body.string());
                        throw new RuntimeException(msg);
                    }
                } catch (IOException e) {
                    throw new RuntimeException("远程调用失败：" + request, e);
                }
            });
            idx++;
        }

        CompletableFuture<Void> voidCompletableFuture = CompletableFuture.allOf(futures);
        try {
            voidCompletableFuture.get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("请求网关[{}]报错", defaultNetProtocol, e);
        }
    }

}
