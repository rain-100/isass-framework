// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.http;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.util.MultiValueMap;
import tools.jackson.databind.ObjectMapper;
import vip.isass.framework.nocode.ServiceRegistry;
import vip.isass.framework.nocode.criteria.ICriteria;
import vip.isass.framework.nocode.entity.EntityAssociation;
import vip.isass.framework.nocode.entity.IEntity;
import vip.isass.framework.nocode.service.IService;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Loads explicitly requested generated associations after a standard nocode query. */
final class NocodeAssociationLoader {

    private static final String QUERY = "association.query";
    private static final String CRITERIA_PREFIX = "association.";

    private final ServiceRegistry services;
    private final ObjectMapper objectMapper;

    NocodeAssociationLoader(ServiceRegistry services, ObjectMapper objectMapper) {
        this.services = services;
        this.objectMapper = objectMapper;
    }

    void load(IService<?, ?> rootService, Object result, MultiValueMap<String, String> query) {
        List<IEntity<?>> parents = parents(rootService.entityClass(), result);
        if (parents.isEmpty()) {
            return;
        }
        Set<String> requested = requestedProperties(query);
        if (requested.isEmpty()) {
            return;
        }
        Map<String, EntityAssociation> associations = associations(rootService.entityClass());
        for (String property : requested) {
            EntityAssociation association = associations.get(property);
            if (association == null) {
                throw new IllegalArgumentException("Unknown association '" + property + "' for "
                        + rootService.entityClass().getSimpleName());
            }
            loadAssociation(parents, association, query);
        }
    }

    private void loadAssociation(
            List<IEntity<?>> parents,
            EntityAssociation association,
            MultiValueMap<String, String> query
    ) {
        IService<?, ?> targetService = services.serviceByEntityClass(association.targetType());
        if (targetService == null) {
            throw new IllegalStateException("Association target has no local nocode service: "
                    + association.targetType().getName());
        }
        List<Object> relationValues = parents.stream()
                .map(parent -> propertyValue(parent, association.localField()))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (relationValues.isEmpty()) {
            parents.forEach(parent -> setProperty(parent, association.property(),
                    association.kind() == EntityAssociation.Kind.MANY ? List.of() : null));
            return;
        }
        Map<String, List<String>> criteriaParameters = criteriaParameters(association.property(), query);
        if (association.kind() == EntityAssociation.Kind.ONE
                && (criteriaParameters.containsKey("pageNum") || criteriaParameters.containsKey("pageSize"))) {
            throw new IllegalArgumentException("Single association does not support pageNum or pageSize: "
                    + association.property());
        }
        ICriteria<?, ?> criteria = createCriteria(targetService, criteriaParameters);
        addRelationConstraint(criteria, association.targetField(), relationValues);
        @SuppressWarnings({"rawtypes", "unchecked"})
        IService rawTargetService = targetService;
        @SuppressWarnings({"rawtypes", "unchecked"})
        ICriteria rawCriteria = criteria;
        List<?> children = criteriaParameters.containsKey("pageNum") || criteriaParameters.containsKey("pageSize")
                ? rawTargetService.findPageByCriteria(rawCriteria).getRecords()
                : rawTargetService.findByCriteria(rawCriteria);
        Map<Object, List<Object>> childrenByKey = new LinkedHashMap<>();
        for (Object child : children) {
            Object key = propertyValue(child, association.targetField());
            if (key != null) {
                childrenByKey.computeIfAbsent(key, ignored -> new ArrayList<>()).add(child);
            }
        }
        for (IEntity<?> parent : parents) {
            List<Object> values = childrenByKey.getOrDefault(propertyValue(parent, association.localField()), List.of());
            Object value = association.kind() == EntityAssociation.Kind.MANY
                    ? values : values.stream().findFirst().orElse(null);
            setProperty(parent, association.property(), value);
        }
    }

    private ICriteria<?, ?> createCriteria(IService<?, ?> service, Map<String, List<String>> parameters) {
        try {
            Object criteria = service.criteriaClass().getDeclaredConstructor().newInstance();
            for (Map.Entry<String, List<String>> entry : parameters.entrySet()) {
                findSetter(criteria.getClass(), entry.getKey()).ifPresent(setter -> invokeSetter(
                        criteria, setter, entry.getValue()));
            }
            return (ICriteria<?, ?>) criteria;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot create association criteria: " + service.criteriaClass().getName(), exception);
        }
    }

    private void addRelationConstraint(ICriteria<?, ?> criteria, String targetField, List<Object> relationValues) {
        String setterName = targetField + "In";
        Method setter = java.util.Arrays.stream(criteria.getClass().getMethods())
                .filter(method -> method.getName().equals("set" + capitalize(setterName)))
                .filter(method -> method.getParameterCount() == 1)
                // Generated criteria also expose a varargs overload. Use the collection overload so
                // all parent relation values are retained instead of converting only the first one.
                .filter(method -> Collection.class.isAssignableFrom(method.getParameterTypes()[0]))
                .findFirst()
                .or(() -> findSetter(criteria.getClass(), setterName))
                .orElseThrow(() ->
                new IllegalStateException("Association target criteria is missing set" + capitalize(setterName)
                        + ": " + criteria.getClass().getName()));
        invokeSetter(criteria, setter, relationValues);
    }

    private void invokeSetter(Object target, Method setter, Object rawValue) {
        try {
            Type parameterType = setter.getGenericParameterTypes()[0];
            Object source = rawValue instanceof List<?> values
                    && !Collection.class.isAssignableFrom(setter.getParameterTypes()[0])
                    ? values.getFirst() : rawValue;
            Object converted = objectMapper.convertValue(source,
                    objectMapper.getTypeFactory().constructType(parameterType));
            setter.invoke(target, converted);
        } catch (ReflectiveOperationException | IllegalArgumentException exception) {
            throw new IllegalArgumentException("Cannot bind association criteria parameter " + setter.getName(), exception);
        }
    }

    private Map<String, EntityAssociation> associations(Class<?> entityClass) {
        try {
            IEntity<?> entity = (IEntity<?>) entityClass.getDeclaredConstructor().newInstance();
            Map<String, EntityAssociation> result = new LinkedHashMap<>();
            for (EntityAssociation association : entity.associations()) {
                result.put(association.property(), association);
            }
            return result;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot inspect associations for " + entityClass.getName(), exception);
        }
    }

    private Set<String> requestedProperties(MultiValueMap<String, String> query) {
        Set<String> properties = new LinkedHashSet<>();
        String requested = query.getFirst(QUERY);
        if (requested != null) {
            for (String property : requested.split(",")) {
                if (!property.isBlank()) {
                    properties.add(property.trim());
                }
            }
        }
        for (String name : query.keySet()) {
            if (!name.startsWith(CRITERIA_PREFIX)) {
                continue;
            }
            int criteriaIndex = name.indexOf(".criteria.");
            if (criteriaIndex > CRITERIA_PREFIX.length()) {
                properties.add(name.substring(CRITERIA_PREFIX.length(), criteriaIndex));
            }
        }
        return properties;
    }

    private Map<String, List<String>> criteriaParameters(String property, MultiValueMap<String, String> query) {
        String prefix = CRITERIA_PREFIX + property + ".criteria.";
        Map<String, List<String>> result = new LinkedHashMap<>();
        query.forEach((name, values) -> {
            if (name.startsWith(prefix) && values != null && !values.isEmpty()) {
                result.put(name.substring(prefix.length()), values);
            }
        });
        return result;
    }

    private List<IEntity<?>> parents(Class<?> entityClass, Object result) {
        if (result == null) {
            return List.of();
        }
        Collection<?> records = result instanceof IPage<?> page ? page.getRecords()
                : result instanceof Collection<?> collection ? collection : List.of(result);
        List<IEntity<?>> parents = new ArrayList<>();
        for (Object record : records) {
            if (entityClass.isInstance(record)) {
                parents.add((IEntity<?>) record);
            }
        }
        return parents;
    }

    private Object propertyValue(Object target, String property) {
        try {
            Method getter = target.getClass().getMethod("get" + capitalize(property));
            return getter.invoke(target);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot read association field '" + property + "'", exception);
        }
    }

    private void setProperty(Object target, String property, Object value) {
        Method setter = findSetter(target.getClass(), property).orElseThrow(() ->
                new IllegalStateException("Cannot write association field '" + property + "'"));
        invokeSetter(target, setter, value);
    }

    private Optional<Method> findSetter(Class<?> type, String property) {
        String name = "set" + capitalize(property);
        return java.util.Arrays.stream(type.getMethods())
                .filter(method -> method.getName().equals(name) && method.getParameterCount() == 1)
                .findFirst();
    }

    private String capitalize(String value) {
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
