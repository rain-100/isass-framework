// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.initialization;

import org.springframework.core.ResolvableType;
import vip.isass.framework.entrypoint.metadata.ServiceDefinition;
import vip.isass.framework.entrypoint.registry.ServiceDefinitionRegistry;
import vip.isass.framework.nocode.initialization.NocodeInitializationDataService.ImportResult;
import vip.isass.framework.nocode.initialization.NocodeInitializationDataService.EntityInfo;
import vip.isass.framework.nocode.service.ICrudService;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Shared routing and failure aggregation for startup, JSON requests and file uploads.
 */
public final class NocodeInitializationImporter {

    private final NocodeInitializationDataService localService;
    private final INocodeInitializationService remoteService;
    private final Map<String, Set<String>> entityOwners;
    private final Map<String, Class<?>> entityTypes;
    private final List<EntityInfo> entityCatalog;

    public NocodeInitializationImporter(NocodeInitializationDataService localService,
                                        INocodeInitializationService remoteService,
                                        ServiceDefinitionRegistry definitions) {
        this.localService = localService;
        this.remoteService = remoteService;
        Map<String, Set<String>> owners = new LinkedHashMap<>();
        Map<String, Class<?>> types = new LinkedHashMap<>();
        Map<String, EntityInfo> catalog = new LinkedHashMap<>();
        for (ServiceDefinition definition : definitions.all()) {
            if (definition.hasNocodeOperations()) {
                owners.computeIfAbsent(definition.resourceName(), ignored -> new LinkedHashSet<>())
                        .add(definition.serviceName());
                if (definition.serviceInterface() != null
                        && ICrudService.class.isAssignableFrom(definition.serviceInterface())) {
                    Class<?> type = ResolvableType.forClass(definition.serviceInterface())
                            .as(ICrudService.class).getGeneric(0).resolve();
                    if (type != null) {
                        types.put(definition.serviceName() + "/" + definition.resourceName(), type);
                        var info = new EntityInfo(definition.serviceName(), definition.contextName(),
                                definition.resourceName(), NocodeInitializationDataService.entityComment(type));
                        catalog.put(definition.key(), info);
                    }
                }
            }
        }
        entityOwners = owners;
        entityTypes = Map.copyOf(types);
        for (EntityInfo info : localService.entities(null)) {
            catalog.put(info.serviceName() + "/" + info.contextName() + "/" + info.entity(), info);
        }
        entityCatalog = catalog.values().stream().sorted(Comparator.comparing(EntityInfo::serviceName)
                .thenComparing(EntityInfo::contextName).thenComparing(EntityInfo::entity)).toList();
    }

    /** The catalog describes registered API types, without querying remote services or business data. */
    public List<EntityInfo> entities() {
        return entityCatalog;
    }

    /** Routes reads once; an addressed owner without a local implementation never forwards back to itself. */
    public Map<String, List<?>> exportData(String receivingServiceName, Collection<String> entityNames) {
        if (entityNames == null || entityNames.isEmpty()) {
            throw new IllegalArgumentException("请至少选择一个实体");
        }
        Map<String, Set<String>> byService = new LinkedHashMap<>();
        for (String entity : entityNames) {
            byService.computeIfAbsent(owner(entity), ignored -> new LinkedHashSet<>()).add(entity);
        }
        Map<String, List<?>> result = new LinkedHashMap<>();
        for (var entry : byService.entrySet()) {
            boolean local = localService.hasLocalService(entry.getKey());
            if (!local && entry.getKey().equals(receivingServiceName)) {
                throw new IllegalArgumentException("导出目标服务未部署本地实体: " + entry.getKey());
            }
            var target = local ? localService : remoteService;
            var exported = target.exportData(entry.getKey(), entry.getValue());
            if (exported == null || !exported.keySet().containsAll(entry.getValue())) {
                throw new IllegalStateException("导出目标服务返回的数据不完整: " + entry.getKey());
            }
            for (String entity : entry.getValue()) {
                if (exported.get(entity) == null) {
                    throw new IllegalStateException("导出目标服务未返回实体数据: " + entity);
                }
                result.put(entity, exported.get(entity));
            }
        }
        return result;
    }

    public Map<String, Class<?>> entityTypes(Collection<String> entityNames) {
        if (entityNames == null || entityNames.isEmpty()) {
            throw new IllegalArgumentException("请至少选择一个实体");
        }
        Map<String, Class<?>> result = new LinkedHashMap<>();
        for (String entity : entityNames) {
            if (entity == null || entity.isBlank()) {
                throw new IllegalArgumentException("实体名不能为空");
            }
            String serviceName = owner(entity);
            Class<?> type = localService.localEntityType(entity);
            if (type == null) {
                type = entityTypes.get(serviceName + "/" + entity);
            }
            if (type == null) {
                throw new IllegalArgumentException("无法解析初始化实体的 Java 类型: " + entity);
            }
            result.putIfAbsent(entity, type);
        }
        return result;
    }

    /** Resolves local and remote API metadata before parsing tabular field values. */
    public Map<String, List<Map<String, Object>>> readDocument(InputStream input, String filename) throws IOException {
        return localService.readDocument(input, filename, entity -> {
            // Unknown or ambiguous resources remain routing failures, without blocking other entities.
            Set<String> candidates = new LinkedHashSet<>(entityOwners.getOrDefault(entity, Set.of()));
            String localOwner = localService.localServiceName(entity);
            if (localOwner != null) {
                candidates.add(localOwner);
            }
            if (candidates.size() != 1) {
                return null;
            }
            Class<?> local = localService.localEntityType(entity);
            return local != null ? local : entityTypes.get(candidates.iterator().next() + "/" + entity);
        });
    }

    public ImportResult importData(Map<String, ? extends Collection<?>> document) {
        return importData(null, document);
    }

    /**
     * The HTTP path identifies the receiving service. A request already addressed to its
     * resolved owner must execute locally or fail, never forward back to that same owner.
     */
    public ImportResult importData(String receivingServiceName,
                                    Map<String, ? extends Collection<?>> document) {
        Map<String, Map<String, Collection<?>>> byService = new LinkedHashMap<>();
        Map<String, String> failures = new LinkedHashMap<>();
        int total = 0;
        if (document == null) {
            return new ImportResult(0, 0, 0, Map.of());
        }
        for (var entry : document.entrySet()) {
            total += entry.getValue() == null ? 0 : entry.getValue().size();
            try {
                String owner = owner(entry.getKey());
                byService.computeIfAbsent(owner, ignored -> new LinkedHashMap<>())
                        .put(entry.getKey(), entry.getValue());
            } catch (RuntimeException exception) {
                failures.put(entry.getKey(), message(exception));
            }
        }
        int inserted = 0;
        int skipped = 0;
        for (var entry : byService.entrySet()) {
            try {
                boolean local = localService.hasLocalService(entry.getKey());
                if (!local && entry.getKey().equals(receivingServiceName)) {
                    throw new IllegalStateException("初始化目标服务未部署本地实体: " + entry.getKey());
                }
                INocodeInitializationService target = local ? localService : remoteService;
                ImportResult result = target.importData(entry.getKey(), entry.getValue());
                if (result == null) {
                    throw new IllegalStateException("初始化目标服务未返回结果: " + entry.getKey());
                }
                inserted += result.inserted();
                skipped += result.skipped();
                failures.putAll(result.failures());
            } catch (RuntimeException exception) {
                for (String entity : entry.getValue().keySet()) {
                    failures.put(entity, message(exception));
                }
            }
        }
        return new ImportResult(total, inserted, skipped, Collections.unmodifiableMap(failures));
    }

    private String owner(String entity) {
        Set<String> candidates = new LinkedHashSet<>(entityOwners.getOrDefault(entity, Set.of()));
        String localOwner = localService.localServiceName(entity);
        if (localOwner != null) {
            candidates.add(localOwner);
        }
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("未知 NoCode 初始化实体: " + entity);
        }
        if (candidates.size() > 1) {
            throw new IllegalArgumentException("NoCode 初始化实体归属不唯一: " + entity + " -> " + candidates);
        }
        return candidates.iterator().next();
    }

    private static String message(RuntimeException exception) {
        return exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
    }
}
