// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.core;

import cn.hutool.core.util.HashUtil;

/**
 * net 模块 redis key
 */
public interface NetRedisKey {

    /**
     * redis key 分片数，将大 key 分割，避免集群模式的 redis 出现数据倾斜
     */
    int partition = 32;

    String NET_PREFIX = "net:";

    /**
     * 会话
     * <p>
     * hash 结构
     * key: net:session:{node}-${hash(sessionId)%32} field: {sessionId} value: 上线时间
     */
    String SESSION_REDIS_KEY_PREFIX = NET_PREFIX + "session:";

    // region 标签

    /**
     * 标签，会话与标签的关系
     * <p>
     * hash 结构
     * key: net:tag:sid:{node}-${hash(sessionId)%32} field {sessionId} value: tag map
     */
    String TAG_REDIS_KEY_PREFIX = NET_PREFIX + "tag:sid:";

    /**
     * 标签，标签键与会话的关系
     * <p>
     * hash 结构
     * key: net:tag:sid:{node}-${hash(sessionId)%32} field {sessionId} value: tag map
     */
    String TAG_PREFIX = NET_PREFIX + "tag:sid:";

    /**
     * 标签 用户
     * <p>
     * hash 结构
     * key: net:tag:uid:{node}-${hash(userId)%32} field: {userId} value: sessionId
     */
    String TAG_USER_REDIS_KEY_PREFIX = NET_PREFIX + "tag:uid:";

    /**
     * 标签 设备
     * <p>
     * hash 结构
     * key: net:tag:did:{node}-${hash(deviceId)%32} field: {deviceId} value: sessionId
     */
    String TAG_DEVICE_REDIS_KEY_PREFIX = NET_PREFIX + "tag:did:";

    // endregion


    /**
     * 各微服务上报 cmd 的 key
     */
    String CMD_COLLECT_KEY = "kernel:net:transfer:cmd";

    /**
     * redis message key (channel)
     * 服务端推送消息给客户端时，使用 redis 的 pubsub 功能中转消息
     */
    String REDIS_PUBSUB_KEY = "kernel:net:transfer:message:s2c";

    /**
     * 客户端发送消息给服务端推，使用 redis 的 stream 功能中转消息
     */
    String REDIS_STREAM_PREFIX_KEY = "kernel:net:transfer:message:c2s:";

    /**
     * 客户端发送消息给服务端推，无法从 cmd 解析出是发送给哪个微服务时，统一使用此 stream
     */
    String REDIS_STREAM_UNKNOWN_SERVICE_KEY = REDIS_STREAM_PREFIX_KEY + "unknown";

    /**
     * redis stream 的消费组名
     */
    String CONSUMER_GROUP = "g";

    // region 中转 tag 用到的 key

    /**
     * redis add tag key
     * 微服务设置会话标签时，使用 redis 的 pubsub 功能中转标签信息
     */
    String ADD_TAG_PUBSUB_KEY = "kernel:net:transfer:tag:add";

    /**
     * redis remove match tag key
     * 微服务删除会话指定标签时，使用 redis 的 pubsub 功能中转标签信息
     */
    String REMOVE_TAG_PUBSUB_KEY = "kernel:net:transfer:tag:removeMatch";

    /**
     * redis remove all tag key
     * 微服务删除全部会话标签时，使用 redis 的 pubsub 功能中转会话 id 的信息
     */
    String REMOVE_ALL_TAG_PUBSUB_KEY = "kernel:net:transfer:tag:removeALl";

    // endregion

    static String formatSessionKey(String node, String sessionId) {
        int index = HashUtil.fnvHash(sessionId) % partition;
        return SESSION_REDIS_KEY_PREFIX + node + "-" + index;
    }

    static String formatTagKey(String node, String sessionId) {
        int index = HashUtil.fnvHash(sessionId) % partition;
        return TAG_REDIS_KEY_PREFIX + node + "-" + index;
    }

    static String formatUserTagKey(String node, String userId) {
        int index = HashUtil.fnvHash(userId) % partition;
        return TAG_USER_REDIS_KEY_PREFIX + node + "-" + index;
    }

}
