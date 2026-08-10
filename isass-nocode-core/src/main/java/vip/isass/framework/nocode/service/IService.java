// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import vip.isass.framework.common.support.api.IsassOrderUtil;
import vip.isass.framework.nocode.criteria.ICriteria;
import vip.isass.framework.nocode.entity.BatchSave;
import vip.isass.framework.nocode.entity.IEntity;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;

public interface IService<E extends IEntity<E>, C extends ICriteria<E, C>> {

    default int getOrder() {
        return IsassOrderUtil.LOWEST_PRECEDENCE;
    }

    // region 元数据（从泛型参数 + 包路径推断）

    /**
     * 解析目标类型（可为 {@code @Service} 实现 Class）的 IService 泛型实参。
     * 不要求传入实例，便于在 Spring BeanDefinition 后置处理阶段复用。
     * 与 {@link #serviceTypeArgs()} 走相同的反射逻辑，但接受 Class 参数。
     */
    public static Type[] resolveServiceTypeArgs(Class<?> beanClass) {
        Class<?> currentClass = beanClass;
        while (currentClass != null && currentClass != Object.class) {
            for (Type iface : currentClass.getGenericInterfaces()) {
                if (iface instanceof ParameterizedType pt
                        && pt.getRawType() instanceof Class<?> rawClass
                        && IService.class.isAssignableFrom(rawClass)) {
                    return pt.getActualTypeArguments();
                }
            }
            currentClass = currentClass.getSuperclass();
        }
        throw new IllegalStateException("Cannot resolve IService type parameters: " + beanClass.getName());
    }

    private Type[] serviceTypeArgs() {
        return resolveServiceTypeArgs(this.getClass());
    }

    @SuppressWarnings("unchecked")
    default Class<E> entityClass() {
        return (Class<E>) serviceTypeArgs()[0];
    }

    @SuppressWarnings("unchecked")
    default Class<C> criteriaClass() {
        return (Class<C>) serviceTypeArgs()[1];
    }

    default String entity() {
        String name = entityClass().getSimpleName();
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    default String service() {
        String pkg = entityClass().getPackageName();
        String[] parts = pkg.split("\\.");
        return parts.length >= 3 ? parts[2] + "-service" : "unknown";
    }

    // endregion

    // region 增

    /** @http POST / */
    E add(E entity);

    /** @http POST /batch */
    Collection<E> addBatch(Collection<E> entities);

    /** @http POST /batch/batchSize/{batchSize} */
    Collection<E> addBatchByBatchSize(Collection<E> entities, int batchSize);

    /** @http POST /absent/criteria */
    E addIfAbsentByCriteria(E entity, C criteria);

    /** @http POST /absent/{uniqueColumns} */
    E addIfAbsentByColumns(E entity, List<String> uniqueColumns);

    /** @http POST /batch/absent/criteria */
    Integer addBatchIfAbsentByCriteria(List<E> entities, C criteria);

    /** @http POST /batch/absent/{uniqueColumns} */
    Integer addBatchIfAbsentByColumns(List<E> entities, List<String> uniqueColumns);

    /** @http POST /add-update/criteria */
    Boolean addOrUpdateByCriteria(E entity, C criteria);

    /** @http POST /add-update/{uniqueColumns} */
    E addOrUpdateByColumns(E entity, List<String> uniqueColumns);

    /** @http POST /add-update/batch/{uniqueColumns} */
    Integer addOrUpdateBatchByColumns(List<E> entities, List<String> uniqueColumns);

    // endregion

    //  region 删

    /** @http DELETE /id/{id} */
    Boolean deleteById(Serializable id);

    /** @http DELETE /{ids} */
    Boolean deleteByIds(Collection<Serializable> ids);

    /** @http DELETE /criteria */
    Boolean deleteByCriteria(C criteria);

    // endregion

    // region 改

    /** @http PUT / */
    Boolean updateById(E entity);

    /** @http PUT /allColumns */
    Boolean updateAllColumnsById(E entity);

    /** @http PUT /exception */
    void updateByIdOrException(E entity);

    /** @http PUT /criteria */
    Boolean updateByCriteria(E entity, C criteria);

    /** @http PUT /criteria/exception */
    void updateByCriteriaOrException(E entity, C criteria);

    /** @http POST /batchSave */
    void batchSave(BatchSave<E> batchSave);

    // endregion

    //  region 查

    /** @http GET /{id} */
    E getById(Serializable id);

    /** @http GET /exception/{id} */
    E getByIdOrException(Serializable id);

    /** @http GET /1/criteria */
    E getByCriteria(C criteria);

    /** @http GET /warn/criteria */
    E getByCriteriaOrWarn(C criteria);

    /** @http GET /exception/criteria */
    E getByCriteriaOrException(C criteria);

    /** @http GET /criteria */
    List<E> findByCriteria(C criteria);

    /** @http GET /page */
    IPage<E> findPageByCriteria(C criteria);

    /** @http GET /all */
    List<E> findAll();

    /** @http GET /count/criteria */
    Integer countByCriteria(C criteria);

    /** @http GET /count/all */
    Integer countAll();

    /** @http GET /present/{id} */
    Boolean isPresentById(Serializable id);

    /** @http GET /present/{propertyName}/{value} */
    Boolean isPresentByColumn(String propertyName, Object value);

    /** @http GET /present/criteria */
    Boolean isPresentByCriteria(C criteria);

    /** @http GET /absent/{propertyName}/{value} */
    Boolean isAbsentByColumn(String propertyName, Object value);

    /** @http GET /absent/criteria */
    Boolean isAbsentByCriteria(C criteria);

    /** @http GET /exception-if-present/criteria */
    void exceptionIfPresentByCriteria(C criteria);

    /** @http GET /exception-if-absent/criteria */
    void exceptionIfAbsentByCriteria(C criteria);

    // endregion

}
