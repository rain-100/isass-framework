package vip.isass.framework.nocode;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import vip.isass.framework.nocode.service.IService;

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

    @SuppressWarnings("rawtypes")
    public ServiceRegistry(List<? extends IService> services) {
        Map<String, IService<?, ?>> map = new LinkedHashMap<>();
        for (IService service : services) {
            String name = service.entity();
            IService<?, ?> existing = map.putIfAbsent(name, service);
            if (existing != null) {
                throw new IllegalStateException(
                        "Duplicate entity name '" + name + "': "
                                + existing.getClass().getName() + " and "
                                + service.getClass().getName());
            }
        }
        this.services = map;
    }

    public Collection<IService<?, ?>> all() {
        return services.values();
    }

    /** 根据 entity 查找对应的实体 Class */
    public Class<?> entityClass(String entity) {
        IService<?, ?> service = services.get(entity);
        return service != null ? service.entityClass() : null;
    }

    /**
     * 根据 service 和 entity 查找并校验 IService。
     *
     * @throws ResponseStatusException 404 如果 entity 不存在或 service 不匹配
     */
    public IService<?, ?> require(String service, String entity) {
        IService<?, ?> localService = services.get(entity);
        if (localService == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Unknown entity: " + entity);
        }
        if (!Objects.equals(service, localService.service())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Entity '" + entity + "' does not belong to service '" + service + "'");
        }
        return localService;
    }
}
