// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.http;

import vip.isass.framework.nocode.criteria.ICriteria;
import vip.isass.framework.nocode.entity.IEntity;
import vip.isass.framework.nocode.service.ILocalService;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Executes complete YAML export profiles without omitting entity fields. */
public class NocodeExportService {

    private final NocodeInitializationDataService dataService;
    private final NocodeInitializationRemoteClient remoteClient;
    private final ObjectMapper objectMapper;
    private final Map<String, NocodeExportProfile> profiles;

    NocodeExportService(
            NocodeInitializationDataService dataService,
            NocodeInitializationRemoteClient remoteClient,
            NocodeExportProfileLoader loader,
            ObjectMapper objectMapper
    ) {
        this.dataService = dataService;
        this.remoteClient = remoteClient;
        this.objectMapper = objectMapper;
        this.profiles = loader.load();
    }

    public List<ProfileInfo> profiles() {
        return profiles.values().stream()
                .map(profile -> new ProfileInfo(profile.getCode(), profile.getName(), profile.getDescription()))
                .sorted(Comparator.comparing(ProfileInfo::code))
                .toList();
    }

    public NocodeExportPackage export(String currentService, NocodeExportRequest request) {
        if (request == null) throw new IllegalArgumentException("export request is required");
        List<NocodeExportPlan> plans = request.getPlans();
        String profileCode = request.getProfileCode();
        if (plans == null || plans.isEmpty()) {
            if (profileCode == null || profileCode.isBlank()) {
                throw new IllegalArgumentException("profileCode or plans is required");
            }
            NocodeExportProfile profile = profiles.get(profileCode);
            if (profile == null) throw new IllegalArgumentException("Unknown export profile: " + profileCode);
            plans = profile.getEntities();
        } else if (profileCode == null || profileCode.isBlank()) {
            profileCode = "ad-hoc";
        } else {
            throw new IllegalArgumentException("profileCode and plans cannot be used together");
        }

        Map<String, List<NocodeExportPlan>> plansByService = new LinkedHashMap<>();
        for (NocodeExportPlan plan : plans) {
            validatePlan(plan);
            plansByService.computeIfAbsent(plan.getService(), ignored -> new ArrayList<>()).add(plan);
        }

        Map<String, Map<String, List<?>>> documents = new LinkedHashMap<>();
        for (Map.Entry<String, List<NocodeExportPlan>> entry : plansByService.entrySet()) {
            String targetService = entry.getKey();
            Map<String, List<?>> document = dataService.hasLocalService(targetService)
                    ? exportLocal(targetService, entry.getValue(), request.getInput())
                    : remoteClient.exportData(targetService, entry.getValue(), request.getInput());
            documents.put(targetService, document);
        }
        return new NocodeExportPackage(1, profileCode, System.currentTimeMillis(), documents);
    }

    /** Restores a complete package through local repositories and one remote call per target service. */
    public Map<String, NocodeInitializationDataService.ImportResult> importPackage(NocodeExportPackage document) {
        if (document == null || document.services() == null || document.services().isEmpty()) return Map.of();
        Map<String, NocodeInitializationDataService.ImportResult> results = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, List<?>>> entry : document.services().entrySet()) {
            String targetService = entry.getKey();
            NocodeInitializationDataService.ImportResult result = dataService.hasLocalService(targetService)
                    ? dataService.importDataWithFailures(targetService, entry.getValue())
                    : remoteClient.importData(targetService, entry.getValue());
            results.put(targetService, result);
        }
        return Map.copyOf(results);
    }

    /** Internal endpoint used by a coordinator; every remote service receives one complete sub-plan. */
    public Map<String, List<?>> exportInternal(
            String serviceName,
            Collection<NocodeExportPlan> plans,
            Map<String, Object> input
    ) {
        for (NocodeExportPlan plan : plans) {
            if (!serviceName.equals(plan.getService())) {
                throw new IllegalArgumentException("Export plan service does not match endpoint: " + plan.getService());
            }
        }
        return exportLocal(serviceName, plans, input == null ? Map.of() : input);
    }

    private Map<String, List<?>> exportLocal(
            String serviceName,
            Collection<NocodeExportPlan> plans,
            Map<String, Object> input
    ) {
        Map<String, List<?>> document = new LinkedHashMap<>();
        for (NocodeExportPlan plan : plans) {
            if (document.containsKey(plan.getEntity())) {
                throw new IllegalArgumentException("Duplicate export entity in one service: " + plan.getEntity());
            }
            document.put(plan.getEntity(), find(serviceName, plan, input, document));
        }
        return document;
    }

    private void validatePlan(NocodeExportPlan plan) {
        if (plan == null || plan.getService() == null || plan.getService().isBlank()
                || plan.getEntity() == null || plan.getEntity().isBlank()) {
            throw new IllegalArgumentException("Each export plan requires service and entity");
        }
    }

    private List<?> find(
            String serviceName,
            NocodeExportPlan plan,
            Map<String, Object> input,
            Map<String, List<?>> exported
    ) {
        ILocalService<?, ?> service = dataService.requiredLocalService(serviceName, plan.getEntity());
        if (plan.getCriteria() == null || plan.getCriteria().isEmpty()) {
            @SuppressWarnings({"rawtypes", "unchecked"})
            List<?> rows = new ArrayList<>(((ILocalService) service).findAll());
            return rows;
        }
        try {
            ICriteria<?, ?> criteria = service.criteriaClass().getDeclaredConstructor().newInstance();
            for (Map.Entry<String, Object> condition : plan.getCriteria().entrySet()) {
                Object value = resolve(condition.getValue(), input, exported);
                // A child plan must not turn an empty parent result into an unrestricted export.
                if (isEmptyExportReference(condition.getValue(), value)) return List.of();
                setCriteria(criteria, condition.getKey(), value);
            }
            @SuppressWarnings({"rawtypes", "unchecked"})
            List<?> rows = ((ILocalService) service).findByCriteria(criteria);
            return rows;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot create nocode criteria: " + service.criteriaClass().getName(), exception);
        }
    }

    private boolean isEmptyExportReference(Object source, Object resolved) {
        return source instanceof String text
                && text.startsWith("${export.")
                && resolved instanceof Collection<?> values
                && values.isEmpty();
    }

    private void setCriteria(ICriteria<?, ?> criteria, String property, Object value) {
        String setterName = "set" + property.substring(0, 1).toUpperCase(Locale.ROOT) + property.substring(1);
        Method setter = java.util.Arrays.stream(criteria.getClass().getMethods())
                .filter(method -> method.getName().equals(setterName) && method.getParameterCount() == 1)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown export criteria: "
                        + criteria.getClass().getSimpleName() + "." + property));
        try {
            setter.invoke(criteria, objectMapper.convertValue(value, setter.getParameterTypes()[0]));
        } catch (ReflectiveOperationException exception) {
            throw new IllegalArgumentException("Cannot set export criteria: " + property, exception);
        }
    }

    private Object resolve(Object value, Map<String, Object> input, Map<String, List<?>> exported) {
        if (!(value instanceof String text) || !text.startsWith("${") || !text.endsWith("}")) return value;
        if (text.startsWith("${input.")) {
            String key = text.substring("${input.".length(), text.length() - 1);
            if (!input.containsKey(key)) throw new IllegalArgumentException("Missing export input: " + key);
            return input.get(key);
        }
        if (!text.startsWith("${export.")) return value;
        String[] parts = text.substring("${export.".length(), text.length() - 1).split("\\.", 2);
        if (parts.length != 2 || !exported.containsKey(parts[0])) {
            throw new IllegalArgumentException("Unknown exported field reference: " + text);
        }
        return exported.get(parts[0]).stream().map(row -> readProperty(row, parts[1])).toList();
    }

    private Object readProperty(Object row, String property) {
        try {
            Method getter = row.getClass().getMethod("get" + property.substring(0, 1).toUpperCase(Locale.ROOT)
                    + property.substring(1));
            return getter.invoke(row);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalArgumentException("Unknown exported entity property: "
                    + row.getClass().getSimpleName() + "." + property, exception);
        }
    }

    public record ProfileInfo(String code, String name, String description) {
    }
}
