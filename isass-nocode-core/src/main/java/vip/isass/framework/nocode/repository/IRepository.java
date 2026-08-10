// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.repository;

import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.baomidou.mybatisplus.core.metadata.IPage;
import vip.isass.framework.nocode.criteria.ICriteria;
import vip.isass.framework.nocode.entity.IEntity;
import vip.isass.framework.nocode.entity.IIdEntity;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Rain
 */
public interface IRepository<E extends IEntity<E>, C extends ICriteria<E, C>> {

    Logger LOGGER = LoggerFactory.getLogger(IRepository.class);

    Map<Class<?>, String> ID_PROPERTY_NAMES = new ConcurrentHashMap<>(64);

    default String getIdPropertyName(Class<?> clazz) {
        return ID_PROPERTY_NAMES.computeIfAbsent(clazz, c -> {
            if (IIdEntity.class.isAssignableFrom(c)) {
                return "id";
            }
            return "";
        });
    }

    // ****************************** 增 start ******************************

    default boolean add(E entity) {
        throw new UnsupportedOperationException();
    }

    default boolean addBatch(Collection<E> entities) {
        throw new UnsupportedOperationException();
    }

    default boolean addBatch(Collection<E> entities, int batchSize) {
        throw new UnsupportedOperationException();
    }

    default E addOrUpdate(E entity, List<String> uniqueColumns) {
        throw new UnsupportedOperationException();
    }

    // ****************************** 删 start ******************************

    default boolean deleteById(Serializable id) {
        throw new UnsupportedOperationException();
    }

    default boolean deleteByIds(Collection<? extends Serializable> ids) {
        throw new UnsupportedOperationException();
    }

    default boolean deleteByCriteria(ICriteria<E, C> criteria) {
        throw new UnsupportedOperationException();
    }

    //****************************** 改 start ******************************

    default boolean updateById(E entity) {
        throw new UnsupportedOperationException();
    }

    default boolean updateAllColumnsById(E entity) {
        throw new UnsupportedOperationException();
    }

    default boolean updateByCriteria(E entity, ICriteria<E, C> criteria) {
        throw new UnsupportedOperationException();
    }

    // ****************************** 查 start ******************************

    default E getEntityById(Serializable id) {
        throw new UnsupportedOperationException();
    }

    default E getByIdOrException(Serializable id) {
        throw new UnsupportedOperationException();
    }

    default E getByCriteria(ICriteria<E, C> criteria) {
        throw new UnsupportedOperationException();
    }

    default E getByCriteriaOrWarn(ICriteria<E, C> criteria) {
        throw new UnsupportedOperationException();
    }

    default E getByCriteriaOrException(ICriteria<E, C> criteria) {
        throw new UnsupportedOperationException();
    }

    default List<E> findByCriteria(ICriteria<E, C> criteria) {
        throw new UnsupportedOperationException();
    }

    default IPage<E> findPageByCriteria(ICriteria<E, C> criteria) {
        throw new UnsupportedOperationException();
    }

    default List<E> findAll() {
        throw new UnsupportedOperationException();
    }

    default Integer countByCriteria(ICriteria<E, C> criteria) {
        throw new UnsupportedOperationException();
    }

    default Integer countAll() {
        throw new UnsupportedOperationException();
    }

    default boolean isPresentById(Serializable id) {
        throw new UnsupportedOperationException();
    }

    default boolean isPresentByColumn(String propertyName, Object value) {
        throw new UnsupportedOperationException();
    }

    default boolean isPresentByCriteria(ICriteria<E, C> criteria) {
        throw new UnsupportedOperationException();
    }

    default void exceptionIfPresentByCriteria(ICriteria<E, C> criteria) {
        throw new UnsupportedOperationException();
    }

    default void exceptionIfAbsentByCriteria(ICriteria<E, C> criteria) {
        throw new UnsupportedOperationException();
    }

    default boolean addIfAbsentByCriteria(E entity, C criteria) {
        throw new UnsupportedOperationException();
    }

    default boolean addIfAbsentByColumns(E entity, List<String> uniqueColumns) {
        throw new UnsupportedOperationException();
    }

}
