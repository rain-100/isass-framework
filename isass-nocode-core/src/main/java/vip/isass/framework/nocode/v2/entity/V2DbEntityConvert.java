package vip.isass.framework.nocode.v2.entity;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.StrUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import vip.isass.framework.common.support.IsassConfig;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Converts between legacy v2 API entities and database entities.
 *
 * @author Rain
 */
@Slf4j
public class V2DbEntityConvert {

    private static String packageName;

    private static final Map<Class<?>, Class<?>> DB_ENTITY_MAP = new ConcurrentHashMap<>();

    public void setPackageName(String packageName) {
        V2DbEntityConvert.packageName = packageName;
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    public static <E extends IV2Entity<E>, EDB extends IV2DbEntity<E, EDB>> EDB convertToDbEntity(E entity) {
        if (entity == null || entity instanceof IV2DbEntity) {
            return (EDB) entity;
        }

        Class<EDB> dbEntityClass = (Class<EDB>) getDbEntityClass(entity.getClass());
        if (dbEntityClass == null) {
            throw new UnsupportedOperationException(StrUtil.format("entity[{}]没有DbEntity的子类实现", entity.getClass().getName()));
        }

        return BeanUtil.copyProperties(entity, dbEntityClass);
    }

    @SuppressWarnings("unchecked")
    public static <E extends IV2Entity<E>, EDB extends IV2DbEntity<E, EDB>> E convertToEntity(EDB entity) {
        return (E) entity;
    }

    @SuppressWarnings("unchecked")
    public static <E extends IV2Entity<E>, EDB extends IV2DbEntity<E, EDB>> List<EDB> convertToEdbEntities(Collection<E> entities) {
        List<EDB> edbEntities = new ArrayList<>(entities.size());
        for (E entity : entities) {
            if (entity instanceof IV2DbEntity) {
                edbEntities.add((EDB) entity);
            } else {
                edbEntities.add(convertToDbEntity(entity));
            }
        }
        return edbEntities;
    }

    public static <E extends IV2Entity<E>, EDB extends IV2DbEntity<E, EDB>> List<E> convertToEntities(Collection<EDB> entities) {
        return entities == null
                ? null
                : entities.stream()
                .map(V2DbEntityConvert::convertToEntity)
                .collect(Collectors.toList());
    }

    public static Class<?> getDbEntityClass(Class<?> entityClass) {
        return DB_ENTITY_MAP.computeIfAbsent(entityClass, clazz -> {
            Class<?> dbClass = findDbEntityClasses(IsassConfig.PACKAGE_NAME, clazz);
            if (dbClass != null) {
                return dbClass;
            }

            if (StrUtil.isBlank(packageName)) {
                log.warn("没有配置 info.package，db 实体映射失败");
            } else if (!IsassConfig.PACKAGE_NAME.equals(packageName)) {
                dbClass = findDbEntityClasses(packageName, clazz);
            }

            if (dbClass == null) {
                log.error("找不到[{}]对应的dbEntityClass", clazz.getName());
            }
            return dbClass;
        });
    }

    static Class<?> findDbEntityClasses(String scanPackageName, Class<?> entityClass) {
        if (entityClass == null || !IV2Entity.class.isAssignableFrom(entityClass)) {
            return null;
        }

        Set<Class<?>> classes = ClassUtil.scanPackageBySuper(scanPackageName, entityClass);
        if (classes.isEmpty()) {
            return findDbEntityClasses(scanPackageName, entityClass.getSuperclass());
        }
        return classes.stream()
                .filter(IV2DbEntity.class::isAssignableFrom)
                .findFirst()
                .orElse(findDbEntityClasses(scanPackageName, entityClass.getSuperclass()));
    }
}
