// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vip.isass.framework.nocode.criteria.ICriteria;
import vip.isass.framework.nocode.entity.BatchSave;
import vip.isass.framework.nocode.entity.IEntity;
import vip.isass.framework.common.support.api.ApiOrder;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public interface IServiceManager<
        E extends IEntity<E>,
        C extends ICriteria<E, C>,
        S extends IService<E, C>
        > extends IService<E, C> {

    Logger LOGGER = LoggerFactory.getLogger(IServiceManager.class);

    @Override
    default int getOrder() {
        return ApiOrder.SERVER_MANAGER;
    }

    List<S> getServices();

    // region 增

    @Override
    default E add(E entity) {
        return ServiceManagerUtil.applyUntilNotNull(getServices(), s -> s.add(entity));
    }

    @Override
    default Collection<E> addBatch(Collection<E> entities) {
        return ServiceManagerUtil.applyUntilNotNull(getServices(), s -> s.addBatch(entities));
    }

    @Override
    default Collection<E> addBatchByBatchSize(Collection<E> entities, int batchSize) {
        return ServiceManagerUtil.applyUntilNotNull(getServices(), s -> s.addBatchByBatchSize(entities, batchSize));
    }

    @Override
    default E addIfAbsentByCriteria(E entity, C criteria) {
        return ServiceManagerUtil.applyUntilNotNull(getServices(), s -> s.addIfAbsentByCriteria(entity, criteria));
    }

    @Override
    default E addIfAbsentByColumns(E entity, List<String> uniqueColumns) {
        return ServiceManagerUtil.applyUntilNotNull(getServices(), s -> s.addIfAbsentByColumns(entity, uniqueColumns));
    }

    @Override
    default Integer addBatchIfAbsentByCriteria(List<E> entities, C criteria) {
        return ServiceManagerUtil.applyUntilNotNull(getServices(), s -> s.addBatchIfAbsentByCriteria(entities, criteria));
    }

    @Override
    default Integer addBatchIfAbsentByColumns(List<E> entities, List<String> uniqueColumns) {
        return ServiceManagerUtil.applyUntilNotNull(getServices(), s -> s.addBatchIfAbsentByColumns(entities, uniqueColumns));
    }

    @Override
    default Boolean addOrUpdateByCriteria(E entity, C criteria) {
        return ServiceManagerUtil.applyUntilNotNull(getServices(), s -> s.addOrUpdateByCriteria(entity, criteria));
    }

    @Override
    default E addOrUpdateByColumns(E entity, List<String> uniqueColumns) {
        return ServiceManagerUtil.applyUntilNotNull(getServices(), s -> s.addOrUpdateByColumns(entity, uniqueColumns));
    }

    @Override
    default Integer addOrUpdateBatchByColumns(List<E> entities, List<String> uniqueColumns) {
        return ServiceManagerUtil.applyUntilNotNull(getServices(), s -> s.addOrUpdateBatchByColumns(entities, uniqueColumns));
    }

    // endregion

    //  region 删

    @Override
    default Boolean deleteById(Serializable id) {
        return ServiceManagerUtil.applyUntilNotNull(getServices(), s -> s.deleteById(id));
    }

    @Override
    default Boolean deleteByIds(Collection<Serializable> ids) {
        return ServiceManagerUtil.applyUntilNotNull(getServices(), s -> s.deleteByIds(ids));
    }

    @Override
    default Boolean deleteByCriteria(C criteria) {
        return ServiceManagerUtil.applyUntilNotNull(getServices(), s -> s.deleteByCriteria(criteria));
    }

    // endregion

    // region 改

    @Override
    default Boolean updateById(E entity) {
        return ServiceManagerUtil.applyUntilNotNull(getServices(), s -> s.updateById(entity));
    }

    @Override
    default Boolean updateAllColumnsById(E entity) {
        return ServiceManagerUtil.applyUntilNotNull(getServices(), s -> s.updateAllColumnsById(entity));
    }

    @Override
    default void updateByIdOrException(E entity) {
        ServiceManagerUtil.consume(getServices(), s -> s.updateByIdOrException(entity));
    }

    @Override
    default Boolean updateByCriteria(E entity, C criteria) {
        return ServiceManagerUtil.applyUntilNotNull(getServices(), s -> s.updateByCriteria(entity, criteria));
    }

    @Override
    default void updateByCriteriaOrException(E entity, C criteria) {
        ServiceManagerUtil.consume(getServices(), s -> s.updateByCriteriaOrException(entity, criteria));
    }

    @Override
    default void batchSave(BatchSave<E> batchSave) {
        ServiceManagerUtil.consume(getServices(), s -> s.batchSave(batchSave));
    }

    // endregion

    //  region 查

    @Override
    default E getById(Serializable id) {
        return ServiceManagerUtil.applyUntilNotNull(getServices(), s -> s.getById(id));
    }

    @Override
    default E getByIdOrException(Serializable id) {
        return ServiceManagerUtil.applyUntilNotNull(getServices(), s -> s.getByIdOrException(id));
    }

    @Override
    default E getByCriteria(C criteria) {
        return ServiceManagerUtil.applyUntilNotNull(getServices(), s -> s.getByCriteria(criteria));
    }

    @Override
    default E getByCriteriaOrWarn(C criteria) {
        return ServiceManagerUtil.applyUntilNotNull(getServices(), s -> s.getByCriteriaOrWarn(criteria));
    }

    @Override
    default E getByCriteriaOrException(C criteria) {
        return ServiceManagerUtil.applyUntilNotNull(getServices(), s -> s.getByCriteriaOrException(criteria));
    }

    @Override
    default List<E> findByCriteria(C criteria) {
        return ServiceManagerUtil.applyUntilNotNull(getServices(), s -> s.findByCriteria(criteria));
    }

    @Override
    default IPage<E> findPageByCriteria(C criteria) {
        return ServiceManagerUtil.applyUntilNotNull(getServices(), s -> s.findPageByCriteria(criteria));
    }

    @Override
    default List<E> findAll() {
        return ServiceManagerUtil.applyUntilNotNull(getServices(), IService::findAll);
    }

    @Override
    default Integer countByCriteria(C criteria) {
        return ServiceManagerUtil.applyUntilNotNull(getServices(), s -> s.countByCriteria(criteria));
    }

    @Override
    default Integer countAll() {
        return ServiceManagerUtil.applyUntilNotNull(getServices(), IService::countAll);
    }

    @Override
    default Boolean isPresentById(Serializable id) {
        return ServiceManagerUtil.applyUntilNotNull(getServices(), s -> s.isPresentById(id));
    }

    @Override
    default Boolean isPresentByColumn(String columnName, Object value) {
        return ServiceManagerUtil.applyUntilNotNull(getServices(), s -> s.isPresentByColumn(columnName, value));
    }

    @Override
    default Boolean isPresentByCriteria(C criteria) {
        return ServiceManagerUtil.applyUntilNotNull(getServices(), s -> s.isPresentByCriteria(criteria));
    }

    @Override
    default Boolean isAbsentByColumn(String columnName, Object value) {
        return ServiceManagerUtil.applyUntilNotNull(getServices(), s -> s.isAbsentByColumn(columnName, value));
    }

    @Override
    default Boolean isAbsentByCriteria(C criteria) {
        return ServiceManagerUtil.applyUntilNotNull(getServices(), s -> s.isAbsentByCriteria(criteria));
    }

    @Override
    default void exceptionIfPresentByCriteria(C criteria) {
        ServiceManagerUtil.consume(getServices(), s -> s.exceptionIfPresentByCriteria(criteria));
    }

    @Override
    default void exceptionIfAbsentByCriteria(C criteria) {
        ServiceManagerUtil.consume(getServices(), s -> s.exceptionIfAbsentByCriteria(criteria));
    }

    // endregion

    default <V> V applyUntilNotNull(Function<S, V> function) {
        return ServiceManagerUtil.applyUntilNotNull(getServices(), function);
    }

    default void consume(Consumer<S> consumer) {
        ServiceManagerUtil.consume(getServices(), consumer);
    }


    default void consumeWithoutException(Consumer<S> consumer) {
        ServiceManagerUtil.consumeWithoutException(getServices(), consumer);
    }

}
