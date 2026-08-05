package vip.isass.framework.nocode;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import vip.isass.framework.nocode.service.IService;
import vip.isass.framework.nocode.service.ILocalApplicationService;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * nocode 服务注册表：启动时扫描所有 IService Bean，构建 entity → IService 映射。
 */
public class ServiceRegistry {

    private final Map<String, IService<?, ?>> services;
    private final Map<String, Object> endpoints;

    @SuppressWarnings("rawtypes")
    public ServiceRegistry(java.util.Collection<? extends IService> services) {
        this(services, List.of());
    }

    @SuppressWarnings("rawtypes")
    public ServiceRegistry(
            java.util.Collection<? extends IService> services,
            java.util.Collection<? extends ILocalApplicationService> applicationServices
    ) {
        Map<String, IService<?, ?>> map = new LinkedHashMap<>();
        Map<String, Object> endpointMap = new LinkedHashMap<>();
        for (IService service : services) {
            String name = service.entity();
            IService<?, ?> existing = map.putIfAbsent(name, service);
            if (existing != null) {
                throw new IllegalStateException(
                        "Duplicate entity name '" + name + "': "
                                + existing.getClass().getName() + " and "
                                + service.getClass().getName());
            }
            register(endpointMap, service.service(), name, service);
        }
        for (ILocalApplicationService service : applicationServices) {
            register(endpointMap, service.service(), service.entity(), service);
        }
        this.services = map;
        this.endpoints = endpointMap;
    }

    public Collection<IService<?, ?>> all() {
        return services.values();
    }

    /** 根据 entity 查找对应的实体 Class */
    public Class<?> entityClass(String entity) {
        IService<?, ?> service = services.get(entity);
        return service != null ? service.entityClass() : null;
    }

    /** Returns the local standard service by its generated entity name. */
    public IService<?, ?> serviceByEntity(String entity) {
        return services.get(entity);
    }

    /** Returns the local standard service for a generated association target. */
    public IService<?, ?> serviceByEntityClass(Class<?> entityClass) {
        return services.values().stream()
                .filter(service -> service.entityClass().equals(entityClass))
                .findFirst()
                .orElse(null);
    }

    /**
     * 根据 service 和 entity 查找并校验 IService。
     *
     * @throws ResponseStatusException 404 如果 entity 不存在或 service 不匹配
     */
    public Object require(String service, String entity) {
        Object localService = endpoints.get(endpointKey(service, entity));
        if (localService == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Unknown nocode endpoint: " + service + "/" + entity);
        }
        return localService;
    }

    /** Returns whether this process exposes the contract through a local service implementation. */
    public boolean contains(String service, String entity) {
        return endpoints.containsKey(endpointKey(service, entity));
    }

    /** Returns whether at least one nocode endpoint is implemented by the named local service. */
    public boolean containsService(String service) {
        String prefix = service + "\u0000";
        return endpoints.keySet().stream().anyMatch(key -> key.startsWith(prefix));
    }

    private void register(Map<String, Object> endpoints, String service, String entity, Object endpoint) {
        String key = endpointKey(service, entity);
        Object existing = endpoints.putIfAbsent(key, endpoint);
        if (existing != null) {
            throw new IllegalStateException("Duplicate nocode endpoint '" + service + "/" + entity + "': "
                    + existing.getClass().getName() + " and " + endpoint.getClass().getName());
        }
    }

    private String endpointKey(String service, String entity) {
        return service + "\u0000" + entity;
    }
}
