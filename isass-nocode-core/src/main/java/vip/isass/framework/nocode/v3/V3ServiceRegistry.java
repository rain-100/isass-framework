package vip.isass.framework.nocode.v3;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import vip.isass.framework.nocode.v3.service.IV3Service;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * v3 服务注册表：启动时扫描所有 IV3Service Bean，构建 entityName → IV3Service 映射。
 */
public class V3ServiceRegistry {

    private final Map<String, IV3Service<?, ?>> services;

    public V3ServiceRegistry(List<IV3Service<?, ?>> services) {
        Map<String, IV3Service<?, ?>> map = new LinkedHashMap<>();
        for (IV3Service<?, ?> service : services) {
            String name = service.entityName();
            IV3Service<?, ?> existing = map.putIfAbsent(name, service);
            if (existing != null) {
                throw new IllegalStateException(
                        "Duplicate entity name '" + name + "': "
                                + existing.getClass().getName() + " and "
                                + service.getClass().getName());
            }
        }
        this.services = map;
    }

    public Collection<IV3Service<?, ?>> all() {
        return services.values();
    }

    /** 根据 entityName 查找对应的实体 Class */
    public Class<?> entityClass(String entityName) {
        IV3Service<?, ?> service = services.get(entityName);
        return service != null ? service.entityClass() : null;
    }

    /**
     * 根据 serviceName 和 entityName 查找并校验 IV3Service。
     *
     * @throws ResponseStatusException 404 如果 entity 不存在或 serviceName 不匹配
     */
    public IV3Service<?, ?> require(String serviceName, String entityName) {
        IV3Service<?, ?> service = services.get(entityName);
        if (service == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Unknown entity: " + entityName);
        }
        if (!Objects.equals(serviceName, service.serviceName())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Entity '" + entityName + "' does not belong to service '" + serviceName + "'");
        }
        return service;
    }
}
