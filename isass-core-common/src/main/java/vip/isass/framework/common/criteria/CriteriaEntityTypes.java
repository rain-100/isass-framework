// SPDX-License-Identifier: LGPL-3.0-only
package vip.isass.framework.common.criteria;

import org.springframework.core.ResolvableType;
import vip.isass.framework.common.entity.IEntity;

import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 泛型解析不依赖数据库适配器；JSON 类型白名单由注册层负责。
 */
public final class CriteriaEntityTypes {
    private static final Map<String, Class<?>> ENTITIES = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Class<?>> CRITERIA = new ConcurrentHashMap<>();
    private record CriteriaType(Class<?> entityClass, String entityType) { }

    private static final ClassValue<String> ENTITY_NAMES = new ClassValue<>() {
        @Override
        protected String computeValue(Class<?> type) {
            String name = type.getSimpleName();
            return Character.toLowerCase(name.charAt(0)) + name.substring(1);
        }
    };
    private static final ClassValue<CriteriaType> CRITERIA_TYPES = new ClassValue<>() {
        @Override
        protected CriteriaType computeValue(Class<?> type) {
            Class<?> entity = ResolvableType.forClass(type).as(ICriteria.class).getGeneric(0).resolve();
            if (entity == null || entity == IEntity.class || !IEntity.class.isAssignableFrom(entity)) {
                return new CriteriaType(IEntity.class, null);
            }
            return new CriteriaType(entity, entityType(entity));
        }
    };

    private CriteriaEntityTypes() {
    }

    public static Class<?> entityClass(ICriteria<?, ?> criteria) {
        if (criteria instanceof EmptyCriteria<?> empty) {
            return empty.entityClass();
        }
        CriteriaType metadata = CRITERIA_TYPES.get(criteria.getClass());
        if (metadata.entityType() == null) {
            throw new IllegalArgumentException("无法解析 Criteria 的实体类型: " + criteria.getClass().getName());
        }
        return metadata.entityClass();
    }

    public static String entityType(ICriteria<?, ?> criteria) {
        if (criteria instanceof EmptyCriteria<?> empty) {
            return entityType(empty.entityClass());
        }
        CriteriaType metadata = CRITERIA_TYPES.get(criteria.getClass());
        if (metadata.entityType() == null) {
            throw new IllegalArgumentException("无法解析 Criteria 的实体类型: " + criteria.getClass().getName());
        }
        return metadata.entityType();
    }

    public static String entityType(Class<?> type) {
        return ENTITY_NAMES.get(type);
    }

    /**
     * 仅接收应用已知的 Criteria Class，不从请求中加载任意类。
     */
    public static void register(Class<?> criteriaClass) {
        if (!ICriteria.class.isAssignableFrom(criteriaClass)
                || criteriaClass == EmptyCriteria.class
                || criteriaClass.isInterface()
                || Modifier.isAbstract(criteriaClass.getModifiers())) {
            return;
        }
        CriteriaType metadata = CRITERIA_TYPES.get(criteriaClass);
        if (metadata.entityType() == null) {
            return;
        }
        Class<?> entity = metadata.entityClass();
        Class<?> previous = ENTITIES.putIfAbsent(metadata.entityType(), entity);
        if (previous != null && previous != entity) {
            throw new IllegalStateException("实体小驼峰名称有歧义: " + metadata.entityType());
        }
        // 同一实体的临时 Criteria 不覆盖已注册的正式类型。
        CRITERIA.putIfAbsent(entity, criteriaClass);
    }

    public static Class<?> requireEntity(String name) {
        Class<?> type = ENTITIES.get(name);
        if (type == null) throw new IllegalArgumentException("未知 entityType: " + name);
        return type;
    }

    public static ICriteria<?, ?> newCriteria(Class<?> entity) {
        Class<?> type = CRITERIA.get(entity);
        if (type == null) throw new IllegalArgumentException("实体未注册具体 Criteria: " + entity.getName());
        return instantiate(type);
    }

    public static ICriteria<?, ?> instantiate(Class<?> type) {
        try {
            return (ICriteria<?, ?>) CriteriaMetadata.constructor(type).newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalArgumentException("Criteria 必须提供无参构造器: " + type.getName(), exception);
        }
    }

    @SuppressWarnings("unchecked")
    public static <E extends IEntity<E>> ICriteria<E, ?> materializeForWrite(ICriteria<E, ?> criteria) {
        return criteria instanceof EmptyCriteria<?>
                ? (ICriteria<E, ?>) newCriteria(entityClass(criteria)) : criteria;
    }
}
