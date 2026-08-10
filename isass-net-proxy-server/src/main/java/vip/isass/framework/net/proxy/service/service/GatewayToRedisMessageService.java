// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.proxy.service.service;

import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
import vip.isass.framework.net.core.NetRedisKey;
import vip.isass.framework.net.core.message.Message;

import java.util.Map;

/**
 * 把网关收到客户端的消息，推送到 redis
 *
 * @author rain
 */
public class GatewayToRedisMessageService {

    private final RedisTemplate<String, Object> redisTemplate;

    public GatewayToRedisMessageService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String createGroup(String key, String group) {
        return redisTemplate.opsForStream().createGroup(key, group);
    }

    public RecordId push(Message message) {
        String streamKey = parseStreamKeyFromCmd(message.getCmd());
        ObjectRecord<String, Message> record = StreamRecords.newRecord()
                .in(streamKey)
                .ofObject(message)
                .withId(RecordId.autoGenerate());
        return redisTemplate.opsForStream().add(record);
    }

    /**
     * 根据 cmd 解析出目标微服务
     * 返回的微服务需要在正在监听的微服务中，避免客户端发送大量无法匹配微服务的 cmd，造成创建大量无意义的 redis stream
     *
     * @param cmd 路由命令
     * @return 微服务名
     */
    private String parseStreamKeyFromCmd(String cmd) {
        Map<String, MessageRedisKeyMapping> mapping = ProxyClientCmdListeningService.getMessageRedisKeyMapping();
        for (Map.Entry<String, MessageRedisKeyMapping> entry : mapping.entrySet()) {
            if (!cmd.startsWith(entry.getKey())) {
                continue;
            }
            return entry.getValue().getRedisKey();
        }

        return NetRedisKey.REDIS_STREAM_UNKNOWN_SERVICE_KEY;
    }

}
