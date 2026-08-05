package vip.isass.framework.nocode.http;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import vip.isass.framework.nocode.ServiceRegistry;
import vip.isass.framework.nocode.entity.IEntity;
import vip.isass.framework.nocode.entity.IIdEntity;
import vip.isass.framework.nocode.service.ILocalService;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Imports and exports standard nocode entities as portable JSON documents. */
public class NocodeInitializationDataService {

    private static final TypeReference<LinkedHashMap<String, List<Map<String, Object>>>> DOCUMENT_TYPE =
            new TypeReference<>() {
            };

    private final ServiceRegistry services;
    private final ObjectMapper objectMapper;

    public NocodeInitializationDataService(ServiceRegistry services, ObjectMapper objectMapper) {
        this.services = services;
        this.objectMapper = objectMapper;
    }

    public ImportResult importResource(InputStream input) throws IOException {
        return importData(readDocument(input));
    }

    public Map<String, List<Map<String, Object>>> readDocument(InputStream input) throws IOException {
        return objectMapper.readValue(input, DOCUMENT_TYPE);
    }

    /** Returns whether the current process has a local standard implementation of the target service. */
    public boolean hasLocalService(String serviceName) {
        return services.containsService(serviceName);
    }

    public ImportResult importData(Map<String, ? extends Collection<?>> document) {
        return importData(null, document);
    }

    /**
     * Imports rows directly through target-service repositories. This deliberately bypasses
     * application-service defaults and CRUD lifecycle listeners: an initialization document is
     * data, not a replay of interactive business operations.
     */
    public ImportResult importData(String serviceName, Map<String, ? extends Collection<?>> document) {
        return importData(serviceName, document, true);
    }

    /** Imports each local entity independently for the HTTP data-migration endpoint. */
    public ImportResult importDataWithFailures(String serviceName, Map<String, ? extends Collection<?>> document) {
        return importData(serviceName, document, false);
    }

    /** Lists only standard entity services implemented by the current microservice. */
    public List<EntityInfo> entities(String serviceName) {
        return services.all().stream()
                .filter(service -> serviceName.equals(service.service()))
                .map(service -> new EntityInfo(service.entity(), entityComment(service.entityClass())))
                .toList();
    }

    public Map<String, List<?>> exportData(String serviceName, Collection<String> entityNames) {
        Map<String, List<?>> document = new LinkedHashMap<>();
        for (String entityName : entityNames) {
            ILocalService<?, ?> service = requiredLocalService(serviceName, entityName);
            @SuppressWarnings({"rawtypes", "unchecked"})
            List<?> rows = new ArrayList<>(((ILocalService) service).findAll());
            document.put(entityName, rows);
        }
        return document;
    }

    private ImportResult importData(
            String serviceName,
            Map<String, ? extends Collection<?>> document,
            boolean failFast
    ) {
        ImportSummary summary = new ImportSummary();
        if (document == null) return summary.result();
        document.forEach((entityName, rows) -> {
            try {
                importRows(serviceName, entityName, rows, summary);
            } catch (RuntimeException exception) {
                if (failFast) throw exception;
                summary.failures.put(entityName, message(exception));
            }
        });
        return summary.result();
    }

    private void importRows(
            String serviceName,
            String entityName,
            Collection<?> rows,
            ImportSummary summary
    ) {
        ResolvedService resolved = resolveService(serviceName, entityName);
        if (resolved == null) throw new IllegalArgumentException("Unknown local nocode entity: " + entityName);
        if (rows == null) return;
        for (Object row : rows) {
            summary.total++;
            IEntity<?> entity = (IEntity<?>) objectMapper.convertValue(row, resolved.entityClass());
            if (alreadyExists(resolved.service(), entity)) {
                summary.skipped++;
                continue;
            }
            add(resolved.service(), entity);
            summary.inserted++;
        }
    }

    private boolean alreadyExists(ILocalService<?, ?> service, IEntity<?> entity) {
        if (!(entity instanceof IIdEntity<?, ?> idEntity) || idEntity.getId() == null) return false;
        @SuppressWarnings({"rawtypes", "unchecked"})
        ILocalService rawService = service;
        return Boolean.TRUE.equals(rawService.isPresentById((Serializable) idEntity.getId()));
    }

    private void add(ILocalService<?, ?> service, IEntity<?> entity) {
        @SuppressWarnings({"rawtypes", "unchecked"})
        vip.isass.framework.nocode.repository.IRepository repository = service.getRepository();
        repository.add(entity);
    }

    private ResolvedService resolveService(String serviceName, String entityName) {
        if (serviceName == null) {
            ILocalService<?, ?> service = localService(services.serviceByEntity(entityName), entityName);
            return service == null ? null : new ResolvedService(service, service.entityClass());
        }
        ILocalService<?, ?> service = requiredLocalService(serviceName, entityName);
        return new ResolvedService(service, service.entityClass());
    }

    private ILocalService<?, ?> requiredLocalService(String serviceName, String entityName) {
        ILocalService<?, ?> service = localService(services.require(serviceName, entityName), entityName);
        if (service == null) {
            throw new IllegalArgumentException("Nocode endpoint is not a local standard entity service: "
                    + serviceName + "/" + entityName);
        }
        return service;
    }

    private ILocalService<?, ?> localService(Object service, String entityName) {
        if (service == null) return null;
        if (!(service instanceof ILocalService<?, ?> localService)) {
            throw new IllegalArgumentException("Nocode entity is not implemented locally: " + entityName);
        }
        return localService;
    }

    private String message(Exception exception) {
        Throwable current = exception;
        while (current.getCause() != null && (current.getMessage() == null || current.getMessage().isBlank())) {
            current = current.getCause();
        }
        return current.getMessage() == null || current.getMessage().isBlank()
                ? current.getClass().getSimpleName() : current.getMessage();
    }

    private String entityComment(Class<?> entityClass) {
        try {
            Field field = entityClass.getField("COMMENT");
            Object value = field.get(null);
            if (value instanceof String comment && !comment.isBlank()) return comment;
        } catch (NoSuchFieldException ignored) {
            // Models generated before COMMENT was introduced remain available during gradual upgrades.
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Cannot read entity comment: " + entityClass.getName(), exception);
        }
        return entityClass.getSimpleName();
    }

    private static final class ImportSummary {
        private int total;
        private int inserted;
        private int skipped;
        private final Map<String, String> failures = new LinkedHashMap<>();

        private ImportResult result() {
            return new ImportResult(total, inserted, skipped, Map.copyOf(failures));
        }
    }

    public record EntityInfo(String entity, String comment) {
    }

    public record ImportResult(int total, int inserted, int skipped, Map<String, String> failures) {
    }

    private record ResolvedService(ILocalService<?, ?> service, Class<?> entityClass) {
    }
}
