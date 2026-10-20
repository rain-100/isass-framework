// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.initialization;

import org.springframework.core.ResolvableType;
import tools.jackson.core.type.TypeReference;
import vip.isass.framework.common.support.JsonUtil;
import vip.isass.framework.entrypoint.annotation.EntrypointInfo;
import vip.isass.framework.common.criteria.ICriteria;
import vip.isass.framework.common.criteria.field.IIdCriteria;
import vip.isass.framework.common.entity.IEntity;
import vip.isass.framework.common.entity.IIdEntity;
import vip.isass.framework.database.core.repository.IRepository;
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
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/** Imports and exports local NoCode entities without replaying interactive CRUD lifecycles. */
public final class NocodeInitializationDataService implements INocodeInitializationService {

    private static final int EXISTING_ID_BATCH_SIZE = 100;
    private static final TypeReference<LinkedHashMap<String, List<Map<String, Object>>>> DOCUMENT_TYPE =
            new TypeReference<>() { };

    private final Map<String, LocalEntity> entities;

    public NocodeInitializationDataService(Collection<ILocalCrudService<?, ?, ?>> services) {
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
                    new LocalEntity(info.serviceName(), info.contextName(), entityClass, service));
            if (previous != null) {
                throw new IllegalStateException("初始化资源名称重复: " + info.resourceName());
            }
        }
        entities = Map.copyOf(resolved);
    }

    public Map<String, List<Map<String, Object>>> readDocument(InputStream input) throws IOException {
        return JsonUtil.readValue(input, DOCUMENT_TYPE);
    }

    /**
     * Reads initialization JSON, Excel or CSV using local entity metadata.
     */
    public Map<String, List<Map<String, Object>>> readDocument(InputStream input, String filename)
            throws IOException {
        return readDocument(input, filename, this::localEntityType);
    }

    Map<String, List<Map<String, Object>>> readDocument(InputStream input, String filename,
                                                      Function<String, Class<?>> entityTypes) throws IOException {
        if (!supportsDocument(filename)) {
            throw new IllegalArgumentException("初始化文件仅支持 .json、.xlsx 或 .csv: " + filename);
        }
        if (filename.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            return NocodeInitializationExcelReader.read(input, entityTypes);
        }
        if (filename.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            return NocodeInitializationCsv.read(input, filename, entityTypes);
        }
        return readDocument(input);
    }

    static boolean supportsDocument(String filename) {
        if (filename == null || filename.startsWith("~$")) {
            return false;
        }
        String normalized = filename.toLowerCase(Locale.ROOT);
        return normalized.endsWith(".json") || normalized.endsWith(".xlsx") || normalized.endsWith(".csv");
    }

    public boolean hasLocalService(String serviceName) {
        return entities.values().stream().anyMatch(entity -> entity.serviceName().equals(serviceName));
    }

    public String localServiceName(String entityName) {
        LocalEntity entity = entities.get(entityName);
        return entity == null ? null : entity.serviceName();
    }

    Class<?> localEntityType(String entityName) {
        LocalEntity entity = entities.get(entityName);
        return entity == null ? null : entity.entityClass();
    }

    public ImportResult importDataWithFailures(String serviceName,
                                               Map<String, ? extends Collection<?>> document) {
        return importData(serviceName, document);
    }

    public List<EntityInfo> entities(String serviceName) {
        return entities.entrySet().stream()
                .filter(entry -> serviceName == null || entry.getValue().serviceName().equals(serviceName))
                .map(entry -> new EntityInfo(entry.getValue().serviceName(), entry.getValue().contextName(),
                        entry.getKey(), entityComment(entry.getValue().entityClass())))
                .toList();
    }

    @Override
    public Map<String, List<?>> exportData(String serviceName, Collection<String> entityNames) {
        Map<String, List<?>> result = new LinkedHashMap<>();
        for (String entityName : entityNames) {
            LocalEntity entity = require(serviceName, entityName);
            result.put(entityName, new ArrayList<>(repository(entity.service()).findAll()));
        }
        return result;
    }

    @Override
    public ImportResult importData(String serviceName, Map<String, ? extends Collection<?>> document) {
        ImportSummary summary = new ImportSummary();
        if (document == null) {
            return summary.result();
        }
        document.forEach((entityName, rows) -> {
            summary.total += rows == null ? 0 : rows.size();
            try {
                importRows(require(serviceName, entityName), rows, summary);
            } catch (RuntimeException exception) {
                summary.failures.put(entityName, message(exception));
            }
        });
        return summary.result();
    }

    private void importRows(LocalEntity resolved, Collection<?> rows, ImportSummary summary) {
        if (rows == null) return;
        List<IEntity<?>> converted = new ArrayList<>(rows.size());
        for (Object row : rows) {
            converted.add((IEntity<?>) JsonUtil.convertValue(row, resolved.entityClass()));
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
    private IRepository repository(ILocalCrudService service) {
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

    static String entityComment(Class<?> entityClass) {
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

    private record LocalEntity(String serviceName, String contextName, Class<?> entityClass,
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

    public record EntityInfo(String serviceName, String contextName, String entity, String comment) { }
    public record ImportResult(int total, int inserted, int skipped, Map<String, String> failures) { }
}
