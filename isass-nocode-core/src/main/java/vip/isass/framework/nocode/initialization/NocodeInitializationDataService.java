// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.initialization;

import org.springframework.core.ResolvableType;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import vip.isass.framework.entrypoint.annotation.EntrypointInfo;
import vip.isass.framework.nocode.criteria.ICriteria;
import vip.isass.framework.nocode.criteria.field.IIdCriteria;
import vip.isass.framework.nocode.entity.IEntity;
import vip.isass.framework.nocode.entity.IIdEntity;
import vip.isass.framework.nocode.service.ICrudService;
import vip.isass.framework.nocode.service.ILocalCrudService;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Imports and exports local NoCode entities without replaying interactive CRUD lifecycles. */
public final class NocodeInitializationDataService {

    private static final int EXISTING_ID_BATCH_SIZE = 100;
    private static final TypeReference<LinkedHashMap<String, List<Map<String, Object>>>> DOCUMENT_TYPE =
            new TypeReference<>() { };

    private final Map<String, LocalEntity> entities;
    private final ObjectMapper objectMapper;

    public NocodeInitializationDataService(Collection<ILocalCrudService<?, ?, ?>> services,
                                           ObjectMapper objectMapper) {
        Map<String, LocalEntity> resolved = new LinkedHashMap<>();
        for (ILocalCrudService<?, ?, ?> service : services) {
            Class<?> serviceInterface = serviceInterface(service.getClass());
            EntrypointInfo info = serviceInterface.getAnnotation(EntrypointInfo.class);
            Class<?> entityClass = ResolvableType.forClass(serviceInterface)
                    .as(ICrudService.class).getGeneric(0).resolve();
            if (entityClass == null) {
                throw new IllegalStateException("无法解析初始化实体类型: " + serviceInterface.getName());
            }
            LocalEntity previous = resolved.putIfAbsent(info.resourceName(),
                    new LocalEntity(info.serviceName(), entityClass, service));
            if (previous != null) {
                throw new IllegalStateException("初始化资源名称重复: " + info.resourceName());
            }
        }
        entities = Map.copyOf(resolved);
        this.objectMapper = objectMapper;
    }

    public Map<String, List<Map<String, Object>>> readDocument(InputStream input) throws IOException {
        return objectMapper.readValue(input, DOCUMENT_TYPE);
    }

    public boolean hasLocalService(String serviceName) {
        return entities.values().stream().anyMatch(entity -> entity.serviceName().equals(serviceName));
    }

    public String localServiceName(String entityName) {
        LocalEntity entity = entities.get(entityName);
        return entity == null ? null : entity.serviceName();
    }

    public ImportResult importData(String serviceName, Map<String, ? extends Collection<?>> document) {
        return importData(serviceName, document, true);
    }

    public ImportResult importDataWithFailures(String serviceName,
                                               Map<String, ? extends Collection<?>> document) {
        return importData(serviceName, document, false);
    }

    public List<EntityInfo> entities(String serviceName) {
        return entities.entrySet().stream()
                .filter(entry -> entry.getValue().serviceName().equals(serviceName))
                .map(entry -> new EntityInfo(entry.getKey(), entityComment(entry.getValue().entityClass())))
                .toList();
    }

    public Map<String, List<?>> exportData(String serviceName, Collection<String> entityNames) {
        Map<String, List<?>> result = new LinkedHashMap<>();
        for (String entityName : entityNames) {
            LocalEntity entity = require(serviceName, entityName);
            result.put(entityName, new ArrayList<>(repository(entity.service()).findAll()));
        }
        return result;
    }

    private ImportResult importData(String serviceName, Map<String, ? extends Collection<?>> document,
                                    boolean failFast) {
        ImportSummary summary = new ImportSummary();
        if (document == null) return summary.result();
        document.forEach((entityName, rows) -> {
            try {
                importRows(require(serviceName, entityName), rows, summary);
            } catch (RuntimeException exception) {
                if (failFast) throw exception;
                summary.failures.put(entityName, message(exception));
            }
        });
        return summary.result();
    }

    private void importRows(LocalEntity resolved, Collection<?> rows, ImportSummary summary) {
        if (rows == null) return;
        List<IEntity<?>> converted = new ArrayList<>(rows.size());
        for (Object row : rows) {
            summary.total++;
            converted.add((IEntity<?>) objectMapper.convertValue(row, resolved.entityClass()));
        }
        Set<Serializable> existingIds = findExistingIds(resolved.service(), converted);
        for (IEntity<?> entity : converted) {
            Serializable id = id(entity);
            if (id != null && !existingIds.add(id)) {
                summary.skipped++;
                continue;
            }
            repository(resolved.service()).add(entity);
            summary.inserted++;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Set<Serializable> findExistingIds(ILocalCrudService<?, ?, ?> service,
                                              Collection<IEntity<?>> rows) {
        List<Serializable> ids = rows.stream().map(this::id).filter(java.util.Objects::nonNull).toList();
        Set<Serializable> existing = new LinkedHashSet<>();
        for (int start = 0; start < ids.size(); start += EXISTING_ID_BATCH_SIZE) {
            int end = Math.min(start + EXISTING_ID_BATCH_SIZE, ids.size());
            ICriteria criteria = service.newCriteria();
            ((IIdCriteria) criteria).setIdIn(ids.subList(start, end));
            for (Object row : service.getRepository().findByCriteria(criteria)) {
                Serializable id = id((IEntity<?>) row);
                if (id != null) existing.add(id);
            }
        }
        return existing;
    }

    private Serializable id(IEntity<?> entity) {
        return entity instanceof IIdEntity<?, ?> idEntity && idEntity.getId() instanceof Serializable id ? id : null;
    }

    private LocalEntity require(String serviceName, String entityName) {
        LocalEntity entity = entities.get(entityName);
        if (entity == null || serviceName != null && !serviceName.equals(entity.serviceName())) {
            throw new IllegalArgumentException("当前进程不存在 NoCode 初始化实体: "
                    + serviceName + "/" + entityName);
        }
        return entity;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private vip.isass.framework.nocode.repository.IRepository repository(ILocalCrudService service) {
        return service.getRepository();
    }

    private Class<?> serviceInterface(Class<?> type) {
        if (type == null || type == Object.class) return null;
        for (Class<?> candidate : type.getInterfaces()) {
            if (candidate.isAnnotationPresent(EntrypointInfo.class)
                    && ICrudService.class.isAssignableFrom(candidate)) return candidate;
            Class<?> nested = serviceInterface(candidate);
            if (nested != null) return nested;
        }
        return serviceInterface(type.getSuperclass());
    }

    private String entityComment(Class<?> entityClass) {
        try {
            Field field = entityClass.getField("COMMENT");
            Object value = field.get(null);
            return value instanceof String comment && !comment.isBlank()
                    ? comment : entityClass.getSimpleName();
        } catch (NoSuchFieldException ignored) {
            return entityClass.getSimpleName();
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("无法读取实体注释: " + entityClass.getName(), exception);
        }
    }

    private String message(Exception exception) {
        Throwable current = exception;
        while (current.getCause() != null && (current.getMessage() == null || current.getMessage().isBlank())) {
            current = current.getCause();
        }
        return current.getMessage() == null || current.getMessage().isBlank()
                ? current.getClass().getSimpleName() : current.getMessage();
    }

    private record LocalEntity(String serviceName, Class<?> entityClass,
                               ILocalCrudService<?, ?, ?> service) { }

    private static final class ImportSummary {
        private int total;
        private int inserted;
        private int skipped;
        private final Map<String, String> failures = new LinkedHashMap<>();
        private ImportResult result() {
            return new ImportResult(total, inserted, skipped, Map.copyOf(failures));
        }
    }

    public record EntityInfo(String entity, String comment) { }
    public record ImportResult(int total, int inserted, int skipped, Map<String, String> failures) { }
}
