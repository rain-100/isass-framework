package vip.isass.framework.nocode.v3;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import vip.isass.framework.nocode.v3.entity.IV3Entity;
import vip.isass.framework.nocode.v3.entity.IV3IdEntity;
import vip.isass.framework.nocode.v3.entity.IV3LogicDeleteEntity;
import vip.isass.framework.nocode.v3.entity.IV3ParentIdEntity;
import vip.isass.framework.nocode.v3.entity.IV3TenantEntity;
import vip.isass.framework.nocode.v3.entity.IV3TraceEntity;
import vip.isass.framework.nocode.v3.entity.IV3VersionEntity;
import vip.isass.framework.nocode.v3.service.IV3Service;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 启动时扫描所有 IV3Service，从实体实现的 IV3* 接口推断表元数据。
 */
public class V3TableMetaRegistrar {

    private final Map<Class<?>, V3TableMeta> metaMap = new LinkedHashMap<>();

    public V3TableMetaRegistrar(V3ServiceRegistry registry) {
        for (IV3Service<?, ?> service : registry.all()) {
            Class<?> entityClass = service.entityClass();
            if (metaMap.containsKey(entityClass)) {
                continue;
            }
            String tableName = resolveTableName(entityClass, V3TablePrefixUtil.get(service.serviceName()), service.entityName());
            V3TableMeta meta = new V3TableMeta().tableName(tableName);
            detectIdType(meta, entityClass);
            detectTraceFields(meta, entityClass);
            detectLogicDelete(meta, entityClass);
            detectVersion(meta, entityClass);
            detectTenant(meta, entityClass);
            detectParentId(meta, entityClass);
            metaMap.put(entityClass, meta);
        }
    }

    public V3TableMeta get(Class<?> entityClass) {
        return metaMap.get(entityClass);
    }

    private String resolveTableName(Class<?> entityClass, String tablePrefix, String entityName) {
        if (entityClass.isAnnotationPresent(com.baomidou.mybatisplus.annotation.TableName.class)) {
            return entityClass.getAnnotation(com.baomidou.mybatisplus.annotation.TableName.class).value();
        }
        String custom = entityTableName(entityClass);
        if (custom != null && !custom.isEmpty()) {
            return custom;
        }
        return tablePrefix + entityName;
    }

    /**
     * 检测实体是否 override 了 IV3Entity#tableName()。
     * 如果 declaredMethod 位于 entityClass 自身（非 IV3Entity），则说明自定义了。
     */
    private String entityTableName(Class<?> entityClass) {
        try {
            java.lang.reflect.Method m = entityClass.getMethod("tableName");
            if (m.getDeclaringClass() != IV3Entity.class) {
                return (String) m.invoke(null);
            }
        } catch (NoSuchMethodException ignored) {
        } catch (Exception e) {
            throw new RuntimeException("Cannot read tableName() from " + entityClass.getName(), e);
        }
        return "";
    }

    private void detectIdType(V3TableMeta meta, Class<?> entityClass) {
        if (!IV3IdEntity.class.isAssignableFrom(entityClass)) {
            return;
        }
        meta.idType(IdType.ASSIGN_ID);
        meta.keyType(resolveKeyType(entityClass));
        meta.idColumnName("id");
    }

    private Class<?> resolveKeyType(Class<?> entityClass) {
        for (Type iface : entityClass.getGenericInterfaces()) {
            if (iface instanceof ParameterizedType pt
                    && pt.getRawType() instanceof Class<?> raw
                    && IV3IdEntity.class.isAssignableFrom(raw)) {
                Type[] args = pt.getActualTypeArguments();
                if (args.length > 0 && args[0] instanceof Class<?> c && c != Object.class) {
                    return c;
                }
            }
        }
        return Long.class;
    }

    private void detectTraceFields(V3TableMeta meta, Class<?> entityClass) {
        if (!IV3TraceEntity.class.isAssignableFrom(entityClass)) {
            return;
        }
        meta.fillFields(Map.of(
                "createUserId", FieldFill.INSERT,
                "createUserName", FieldFill.INSERT,
                "createTime", FieldFill.INSERT,
                "modifyUserId", FieldFill.INSERT_UPDATE,
                "modifyUserName", FieldFill.INSERT_UPDATE,
                "modifyTime", FieldFill.INSERT_UPDATE
        ));
    }

    private void detectLogicDelete(V3TableMeta meta, Class<?> entityClass) {
        if (IV3LogicDeleteEntity.class.isAssignableFrom(entityClass)) {
            meta.logicDeleteField("deleteFlag");
        }
    }

    private void detectVersion(V3TableMeta meta, Class<?> entityClass) {
        if (IV3VersionEntity.class.isAssignableFrom(entityClass)) {
            meta.versionField("version");
        }
    }

    private void detectTenant(V3TableMeta meta, Class<?> entityClass) {
        if (IV3TenantEntity.class.isAssignableFrom(entityClass)) {
            meta.tenantIdField("tenantId");
        }
    }

    private void detectParentId(V3TableMeta meta, Class<?> entityClass) {
        if (IV3ParentIdEntity.class.isAssignableFrom(entityClass)) {
            meta.parentIdField("parentId");
        }
    }
}
