// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.proxy.core;

import tools.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import vip.isass.framework.common.support.JsonUtil;
import vip.isass.framework.net.core.NetRedisKey;
import vip.isass.framework.net.core.message.CmdCollectDto;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 路由命令 redis 服务
 *
 * @author rain
 */
@Slf4j
public class CmdRedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final TypeReference<Map<String, List<CmdCollectDto>>> MAP_TYPE_REFERENCE = new TypeReference<Map<String, List<CmdCollectDto>>>() {
    };

    public CmdRedisService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private static final TypeReference<List<CmdCollectDto>> LIST_TYPE_REFERENCE = new TypeReference<List<CmdCollectDto>>() {
    };

    public Map<String, List<CmdCollectDto>> findCommands() {
        Map<String, List<Object>> map = redisTemplate.<String, List<Object>>opsForHash().entries(NetRedisKey.CMD_COLLECT_KEY);
        return JsonUtil.DEFAULT_INSTANCE.convertValue(map, MAP_TYPE_REFERENCE);
    }

    public List<CmdCollectDto> findCommands(String applicationName) {
        List<Object> list = redisTemplate.<String, List<Object>>opsForHash().get(NetRedisKey.CMD_COLLECT_KEY, applicationName);
        return JsonUtil.DEFAULT_INSTANCE.convertValue(list, LIST_TYPE_REFERENCE);
    }

    public void put(String applicationName, Collection<CmdCollectDto> cmdRegisters) {
        redisTemplate.opsForHash().put(NetRedisKey.CMD_COLLECT_KEY, applicationName, cmdRegisters);
        redisTemplate.expire(NetRedisKey.CMD_COLLECT_KEY, 2, TimeUnit.DAYS);
    }

}
