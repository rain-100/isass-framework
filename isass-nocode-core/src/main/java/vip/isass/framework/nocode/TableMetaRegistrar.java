package vip.isass.framework.nocode;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.core.handlers.PostInitTableInfoHandler;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.extension.handlers.Jackson3TypeHandler;
import org.apache.ibatis.session.Configuration;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import vip.isass.framework.nocode.entity.IEntity;
import vip.isass.framework.nocode.entity.IIdEntity;
import vip.isass.framework.nocode.entity.ILogicDeleteEntity;
import vip.isass.framework.nocode.entity.IParentIdEntity;
import vip.isass.framework.nocode.entity.ITenantEntity;
import vip.isass.framework.nocode.entity.ITraceEntity;
import vip.isass.framework.nocode.entity.IVersionEntity;
import vip.isass.framework.nocode.service.IService;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 启动时扫描所有 {@link IService} 实现类的 BeanDefinition，
 * 从其泛型签名解析实体类，再根据实体实现的 I* 接口契约推断表元数据，
 * 通过 {@link PostInitTableInfoHandler} 将表名、主键、逻辑删除、乐观锁等注入到 MyBatis-Plus 的 TableInfo。
 *
 * <p>设计要点：
 * <ul>
 *     <li>实现为 {@link BeanDefinitionRegistryPostProcessor}，在 {@code ConfigurationClassPostProcessor}
 *         完成 {@code @ComponentScan} 之后、任何业务 Bean 实例化之前运行，保证后续
 *         {@code SqlSessionFactory} 构建 TableInfo 时元数据已经就绪。</li>
 *     <li>仅使用 {@link BeanDefinitionRegistry#getBeanDefinitionNames()} +
 *         {@link BeanDefinition#getBeanClassName()} + 静态泛型解析，
 *         <b>不实例化任何 IService Bean</b>，规避 SqlSessionFactory 创建时序竞争。</li>
 *     <li>依赖 Spring 自身的 {@code @ComponentScan}（业务项目自行配置的扫描包）发现 {@code @Service} Bean，
 *         <b>无需任何固定包名或额外配置</b>，天然支持不同公司项目。</li>
 * </ul>
 *
 * <p>表名解析顺序：
 * <ol>
 *     <li>实体类上的 {@code @TableName} 注解（兼容遗留写法）</li>
 *     <li>实体覆盖 {@link IEntity#tableName()} 的返回值（推荐方式，api 模块无需引入 MP 注解）</li>
 *     <li>{@link TablePrefixUtil} 注册前缀 + 实体名下划线形式（兜底）</li>
 * </ol>
 */
public class TableMetaRegistrar
        implements BeanDefinitionRegistryPostProcessor, PostInitTableInfoHandler {

    private static final Map<Class<?>, TableMeta> metaMap = new LinkedHashMap<>();

    /**
     * Spring Bean 构造：MybatisPlusAutoConfiguration 通过 getBeanThen() 从容器中查找
     * PostInitTableInfoHandler Bean 注入到 GlobalConfig。
     */
    public TableMetaRegistrar() {
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
        // ConfigurationClassPostProcessor (PriorityOrdered, HIGHEST_PRECEDENCE) 已先于此 BDRPP 跑完，
        // 此时 @Service IService 的 bean definition 全部注册，且尚未实例化。
        registerFromBeanDefinitions(registry);
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        // no-op
    }

    private static void registerFromBeanDefinitions(BeanDefinitionRegistry registry) {
        for (String beanName : registry.getBeanDefinitionNames()) {
            BeanDefinition bd;
            try {
                bd = registry.getBeanDefinition(beanName);
            } catch (Exception e) {
                continue;
            }
            String className = bd.getBeanClassName();
            if (className == null || className.isBlank()) {
                // 可能是 FactoryBean / 通过 factory-method 注册的定义，跳过；
                //  业务 Service 均为普通 @Service，会有具体的 beanClassName。
                continue;
            }
            Class<?> beanClass;
            try {
                beanClass = Class.forName(
                        className, false, TableMetaRegistrar.class.getClassLoader());
            } catch (Throwable t) {
                // 某些 classpath 上的类可能不在此模块可见，忽略
                continue;
            }
            if (!IService.class.isAssignableFrom(beanClass)
                    || beanClass.isInterface()) {
                continue;
            }
            Class<?> entityClass = resolveEntityClass(beanClass);
            if (entityClass == null || !IEntity.class.isAssignableFrom(entityClass)) {
                continue;
            }
            if (metaMap.containsKey(entityClass)) {
                continue;
            }
            TableMeta meta = new TableMeta().tableName(resolveTableName(entityClass));
            detectIdType(meta, entityClass);
            detectTraceFields(meta, entityClass);
            detectLogicDelete(meta, entityClass);
            detectVersion(meta, entityClass);
            detectTenant(meta, entityClass);
            detectParentId(meta, entityClass);
            metaMap.put(entityClass, meta);
        }
    }

    /**
     * 通过 {@link IService#resolveServiceTypeArgs(Class)} 解析首个泛型实参（实体 Class）。
     * 该实参必须为具体 Class，跳过类型变量（E）等非具体形式（说明该 IService 未参数化，无法推断实体类型）。
     */
    private static Class<?> resolveEntityClass(Class<?> beanClass) {
        try {
            Type[] args = IService.resolveServiceTypeArgs(beanClass);
            if (args == null || args.length < 1) {
                return null;
            }
            Type first = args[0];
            if (first instanceof Class<?> c && c != Object.class) {
                return c;
            }
            return null;
        } catch (Throwable t) {
            return null;
        }
    }

    public static TableMeta get(Class<?> entityClass) {
        return metaMap.get(entityClass);
    }

    /**
     * MP 初始化每张表后回调，从 metaMap 注入 tableName / idType / keyType / keyColumn / keyProperty / logicDelete / version。
     * TableInfo 的 setter 均为包级私有，通过反射调用。
     */
    @Override
    public void postTableInfo(TableInfo tableInfo, Configuration configuration) {
        TableMeta meta = metaMap.get(tableInfo.getEntityType());
        if (meta == null) {
            return;
        }
        invokeSetter(tableInfo, "setTableName", String.class, meta.tableName());
        invokeSetter(tableInfo, "setIdType", IdType.class, meta.idType());
        invokeSetter(tableInfo, "setKeyType", Class.class, meta.keyType());
        invokeSetter(tableInfo, "setKeyColumn", String.class, meta.idColumnName());
        invokeSetter(tableInfo, "setKeyProperty", String.class, meta.idColumnName());

        String lf = meta.logicDeleteField();
        if (lf != null) {
            setTableInfoField(tableInfo, "withLogicDelete", Boolean.TRUE);
            for (TableFieldInfo fi : tableInfo.getFieldList()) {
                if (lf.equals(fi.getProperty())) {
                    setTableInfoField(tableInfo, "logicDeleteFieldInfo", fi);
                    setTableFieldInfoField(fi, "logicDelete", Boolean.TRUE);
                    setTableFieldInfoField(fi, "logicNotDeleteValue",
                            ILogicDeleteEntity.NOT_DELETED_VALUE);
                    setTableFieldInfoField(fi, "logicDeleteValue",
                            ILogicDeleteEntity.DELETED_VALUE);
                    break;
                }
            }
        }

        String vf = meta.versionField();
        if (vf != null) {
            setTableInfoField(tableInfo, "withVersion", Boolean.TRUE);
            for (TableFieldInfo fi : tableInfo.getFieldList()) {
                if (vf.equals(fi.getProperty())) {
                    setTableInfoField(tableInfo, "versionFieldInfo", fi);
                    break;
                }
            }
        }
    }

    /**
     * MP 初始化每个字段后回调，注入 fieldFill。
     */
    @Override
    public void postFieldInfo(TableFieldInfo fieldInfo, Configuration configuration) {
        TableMeta meta = metaMap.get(fieldInfo.getField().getDeclaringClass());
        if (meta == null) {
            return;
        }
        FieldFill fill = meta.fillFields().get(fieldInfo.getProperty());
        if (fill != null) {
            setTableFieldInfoFill(fieldInfo, fill,
                    fill == FieldFill.INSERT || fill == FieldFill.INSERT_UPDATE,
                    fill == FieldFill.UPDATE || fill == FieldFill.INSERT_UPDATE);
        }
        if (isJsonValue(fieldInfo.getField())) {
            String mapping = ",typeHandler=" + Jackson3TypeHandler.class.getName();
            setTableFieldInfoField(fieldInfo, "el", fieldInfo.getProperty() + mapping);
            setTableFieldInfoField(fieldInfo, "mapping", mapping.substring(1));
            setTableFieldInfoField(fieldInfo, "typeHandler", Jackson3TypeHandler.class);
        }
    }

    /** Collection and map fields are persisted as JSON without leaking ORM annotations into models. */
    private static boolean isJsonValue(Field field) {
        return Map.class.isAssignableFrom(field.getType())
                || Collection.class.isAssignableFrom(field.getType());
    }

    private static void invokeSetter(Object target, String methodName, Class<?> paramType, Object value) {
        if (value == null) {
            return;
        }
        try {
            Method m = target.getClass().getDeclaredMethod(methodName, paramType);
            m.setAccessible(true);
            m.invoke(target, value);
        } catch (Exception ignored) {
        }
    }

    @SuppressWarnings("SameParameterValue")
    private static void setTableInfoField(TableInfo tableInfo, String fieldName, Object value) {
        try {
            Field f = TableInfo.class.getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(tableInfo, value);
        } catch (Exception ignored) {
        }
    }

    @SuppressWarnings("SameParameterValue")
    private static void setTableFieldInfoFill(TableFieldInfo fieldInfo, FieldFill fill,
                                              boolean withInsert, boolean withUpdate) {
        try {
            Field ff = TableFieldInfo.class.getDeclaredField("fieldFill");
            ff.setAccessible(true);
            ff.set(fieldInfo, fill);

            Field wi = TableFieldInfo.class.getDeclaredField("withInsertFill");
            wi.setAccessible(true);
            wi.set(fieldInfo, withInsert);

            Field wu = TableFieldInfo.class.getDeclaredField("withUpdateFill");
            wu.setAccessible(true);
            wu.set(fieldInfo, withUpdate);
        } catch (Exception ignored) {
        }
    }

    private static void setTableFieldInfoField(TableFieldInfo fieldInfo, String fieldName, Object value) {
        try {
            Field field = TableFieldInfo.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(fieldInfo, value);
        } catch (Exception ignored) {
        }
    }

    private static String resolveTableName(Class<?> entityClass) {
        if (entityClass.isAnnotationPresent(com.baomidou.mybatisplus.annotation.TableName.class)) {
            return entityClass.getAnnotation(com.baomidou.mybatisplus.annotation.TableName.class).value();
        }
        String custom = entityTableName(entityClass);
        if (custom != null && !custom.isEmpty()) {
            return custom;
        }
        String prefix = TablePrefixUtil.get(deriveServiceName(entityClass));
        String entity = deriveEntityName(entityClass);
        return prefix + StrUtil.toUnderlineCase(entity).toLowerCase();
    }

    /**
     * 检测实体是否 override 了 IEntity#tableName()。
     * 如果 declaredMethod 位于 entityClass 自身（非 IEntity），则说明自定义了。
     */
    private static String entityTableName(Class<?> entityClass) {
        try {
            Method m = entityClass.getMethod("tableName");
            if (m.getDeclaringClass() != IEntity.class) {
                Object instance = entityClass.getDeclaredConstructor().newInstance();
                return (String) m.invoke(instance);
            }
        } catch (NoSuchMethodException ignored) {
        } catch (Exception e) {
            throw new RuntimeException("Cannot read tableName() from " + entityClass.getName(), e);
        }
        return "";
    }

    private static String deriveEntityName(Class<?> entityClass) {
        String name = entityClass.getSimpleName();
        if (name.isEmpty()) {
            return "";
        }
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    private static String deriveServiceName(Class<?> entityClass) {
        String pkg = entityClass.getPackageName();
        String[] parts = pkg.split("\\.");
        return parts.length >= 3 ? parts[2] + "-service" : "unknown";
    }

    private static void detectIdType(TableMeta meta, Class<?> entityClass) {
        if (!IIdEntity.class.isAssignableFrom(entityClass)) {
            return;
        }
        meta.idType(IdType.ASSIGN_ID);
        meta.keyType(resolveKeyType(entityClass));
        meta.idColumnName("id");
    }

    private static Class<?> resolveKeyType(Class<?> entityClass) {
        for (Type iface : entityClass.getGenericInterfaces()) {
            if (iface instanceof ParameterizedType pt
                    && pt.getRawType() instanceof Class<?> raw
                    && IIdEntity.class.isAssignableFrom(raw)) {
                Type[] args = pt.getActualTypeArguments();
                if (args.length > 0 && args[0] instanceof Class<?> c && c != Object.class) {
                    return c;
                }
            }
        }
        return Long.class;
    }

    private static void detectTraceFields(TableMeta meta, Class<?> entityClass) {
        if (!ITraceEntity.class.isAssignableFrom(entityClass)) {
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

    private static void detectLogicDelete(TableMeta meta, Class<?> entityClass) {
        if (ILogicDeleteEntity.class.isAssignableFrom(entityClass)) {
            meta.logicDeleteField("deleteFlag");
        }
    }

    private static void detectVersion(TableMeta meta, Class<?> entityClass) {
        if (IVersionEntity.class.isAssignableFrom(entityClass)) {
            meta.versionField("version");
        }
    }

    private static void detectTenant(TableMeta meta, Class<?> entityClass) {
        if (ITenantEntity.class.isAssignableFrom(entityClass)) {
            meta.tenantIdField("tenantId");
        }
    }

    private static void detectParentId(TableMeta meta, Class<?> entityClass) {
        if (IParentIdEntity.class.isAssignableFrom(entityClass)) {
            meta.parentIdField("parentId");
        }
    }
}
