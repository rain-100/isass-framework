// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vip.isass.framework.common.exception.AbsentException;
import vip.isass.framework.common.security.AuthenticatedPrincipal;
import vip.isass.framework.common.security.CurrentPrincipalUtil;
import vip.isass.framework.common.support.api.ApiOrder;
import vip.isass.framework.nocode.criteria.ICriteria;
import vip.isass.framework.nocode.criteria.type.IWhereConditionCriteria;
import vip.isass.framework.nocode.entity.BatchSave;
import vip.isass.framework.nocode.lifecycle.CrudLifecycleRegistry;
import vip.isass.framework.nocode.lifecycle.CrudOperation;
import vip.isass.framework.nocode.entity.IEntity;
import vip.isass.framework.nocode.entity.IIdEntity;
import vip.isass.framework.nocode.entity.ITenantEntity;
import vip.isass.framework.nocode.repository.IRepository;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * @author rain
 */
public interface ILocalService<
        E extends IEntity<E>,
        C extends ICriteria<E, C>
        > extends IService<E, C> {

    Logger LOGGER = LoggerFactory.getLogger(ILocalService.class);

    IRepository<E, C> getRepository();

    @Override
    default int getOrder() {
        return ApiOrder.LOCAL_SERVICE;
    }

    // region 增

    default E add(E entity) {
        prepareForInsert(entity);
        return CrudLifecycleRegistry.execute(this, CrudOperation.ADD, "add", new Object[]{entity}, () -> {
            getRepository().add(entity);
            return entity;
        });
    }

    default Collection<E> addBatch(Collection<E> entities) {
        prepareForInsert(entities);
        return CrudLifecycleRegistry.execute(this, CrudOperation.ADD_BATCH, "addBatch", new Object[]{entities}, () -> {
            getRepository().addBatch(entities);
            return entities;
        });
    }

    @Override
    default Collection<E> addBatchByBatchSize(Collection<E> entities, int batchSize) {
        prepareForInsert(entities);
        return CrudLifecycleRegistry.execute(this, CrudOperation.ADD_BATCH, "addBatchByBatchSize", new Object[]{entities, batchSize}, () -> {
            getRepository().addBatch(entities, batchSize);
            return entities;
        });
    }

    @Override
    default E addIfAbsentByCriteria(E entity, C criteria) {
        return CrudLifecycleRegistry.execute(this, CrudOperation.ADD_IF_ABSENT, "addIfAbsentByCriteria", new Object[]{entity, criteria}, () -> {
            if (this.isAbsentByCriteria(criteria)) return this.add(entity);
            return null;
        });
    }

    @Override
    default E addIfAbsentByColumns(E entity, List<String> uniqueColumns) {
        prepareForInsert(entity);
        return CrudLifecycleRegistry.execute(this, CrudOperation.ADD_IF_ABSENT, "addIfAbsentByColumns", new Object[]{entity, uniqueColumns}, () -> {
            if (getRepository().addIfAbsentByColumns(entity, uniqueColumns)) return entity;
            return null;
        });
    }

    @Override
    default Integer addBatchIfAbsentByCriteria(List<E> entities, C criteria) {
        prepareForInsert(entities);
        return CrudLifecycleRegistry.execute(this, CrudOperation.ADD_IF_ABSENT, "addBatchIfAbsentByCriteria", new Object[]{entities, criteria}, () -> {
            int count = 0;
            if (!(criteria instanceof IWhereConditionCriteria)) throw new UnsupportedOperationException("criteria不是WhereConditionCriteria，请检查代码");
            if (!((IWhereConditionCriteria) criteria).hasConditions()) throw new IllegalArgumentException("请至少设置1个条件");
            if (this.isPresentByCriteria(criteria)) return 0;
            if (getRepository().addBatch(entities)) count++;
            return count;
        });
    }

    @Override
    default Integer addBatchIfAbsentByColumns(List<E> entities, List<String> uniqueColumns) {
        prepareForInsert(entities);
        return CrudLifecycleRegistry.execute(this, CrudOperation.ADD_IF_ABSENT, "addBatchIfAbsentByColumns", new Object[]{entities, uniqueColumns}, () -> {
            int count = 0;
            for (E entity : entities) if (getRepository().addIfAbsentByColumns(entity, uniqueColumns)) count++;
            return count;
        });
    }

    @Override
    default Boolean addOrUpdateByCriteria(E entity, C criteria) {
        return CrudLifecycleRegistry.execute(this, CrudOperation.ADD_OR_UPDATE, "addOrUpdateByCriteria", new Object[]{entity, criteria}, () -> {
            Boolean update = updateByCriteria(entity, criteria);
            if (!update) add(entity);
            return true;
        });
    }

    @Override
    default E addOrUpdateByColumns(E entity, List<String> uniqueColumns) {
        prepareForInsert(entity);
        return CrudLifecycleRegistry.execute(this, CrudOperation.ADD_OR_UPDATE, "addOrUpdateByColumns", new Object[]{entity, uniqueColumns}, () -> getRepository().addOrUpdate(entity, uniqueColumns));
    }

    @Override
    default Integer addOrUpdateBatchByColumns(List<E> entities, List<String> uniqueColumns) {
        prepareForInsert(entities);
        return CrudLifecycleRegistry.execute(this, CrudOperation.ADD_OR_UPDATE, "addOrUpdateBatchByColumns", new Object[]{entities, uniqueColumns}, () -> {
            for (E entity : entities) getRepository().addOrUpdate(entity, uniqueColumns);
            return entities.size();
        });
    }

    /** Fills the current tenant before MyBatis-Plus builds dynamic insert SQL. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    default E prepareForInsert(E entity) {
        if (entity instanceof ITenantEntity tenantEntity && tenantEntity.getTenantId() == null) {
            AuthenticatedPrincipal principal = CurrentPrincipalUtil.getPrincipal();
            tenantEntity.setTenantId(principal == null || principal.getTenantId() == null ? 0L : principal.getTenantId());
        }
        return entity;
    }

    default Collection<E> prepareForInsert(Collection<E> entities) {
        if (entities != null) {
            entities.forEach(this::prepareForInsert);
        }
        return entities;
    }

    // endregion

    //  region 删

    default Boolean deleteById(Serializable id) {
        return CrudLifecycleRegistry.execute(this, CrudOperation.DELETE, "deleteById", new Object[]{id}, () -> getRepository().deleteById(id));
    }

    default Boolean deleteByIds(Collection<Serializable> ids) {
        return CrudLifecycleRegistry.execute(this, CrudOperation.DELETE, "deleteByIds", new Object[]{ids}, () -> getRepository().deleteByIds(ids));
    }

    default Boolean deleteByCriteria(C criteria) {
        return CrudLifecycleRegistry.execute(this, CrudOperation.DELETE, "deleteByCriteria", new Object[]{criteria}, () -> {
            exceptionIfHaveNoCondition(criteria);
            return getRepository().deleteByCriteria(criteria);
        });
    }

    // endregion

    // region 改

    default Boolean updateById(E entity) {
        return CrudLifecycleRegistry.execute(this, CrudOperation.UPDATE, "updateById", new Object[]{entity}, () -> {
            Assert.isTrue(entity instanceof IIdEntity, "不支持非id类型的对象执行'根据id更新'的操作");
            return getRepository().updateById(entity);
        });
    }

    default Boolean updateAllColumnsById(E entity) {
        return CrudLifecycleRegistry.execute(this, CrudOperation.UPDATE, "updateAllColumnsById", new Object[]{entity}, () -> getRepository().updateAllColumnsById(entity));
    }

    default void updateByIdOrException(E entity) {
        CrudLifecycleRegistry.execute(this, CrudOperation.UPDATE, "updateByIdOrException", new Object[]{entity}, () -> {
            if (!updateById(entity)) throw new AbsentException("更新失败，记录不存在");
            return null;
        });
    }

    default Boolean updateByCriteria(E entity, C criteria) {
        return CrudLifecycleRegistry.execute(this, CrudOperation.UPDATE, "updateByCriteria", new Object[]{entity, criteria}, () -> {
            exceptionIfHaveNoCondition(criteria);
            return getRepository().updateByCriteria(entity, criteria);
        });
    }

    default void updateByCriteriaOrException(E entity, C criteria) {
        CrudLifecycleRegistry.execute(this, CrudOperation.UPDATE, "updateByCriteriaOrException", new Object[]{entity, criteria}, () -> {
            if (!getRepository().updateByCriteria(entity, criteria)) throw new AbsentException("更新失败，记录不存在");
            return null;
        });
    }

    @Override
    default void batchSave(BatchSave<E> batchSave) {
        CrudLifecycleRegistry.execute(this, CrudOperation.BATCH_SAVE, "batchSave", new Object[]{batchSave}, () -> {
            if (batchSave == null) return null;
            addBatch(batchSave.getAddEntities());
            if (CollUtil.isNotEmpty(batchSave.getUpdateEntities())) batchSave.getUpdateEntities().forEach(this::updateById);
            deleteByIds(batchSave.getDeleteIds());
            return null;
        });
    }

    // endregion

    //  region 查

    default E getById(Serializable id) {
        Assert.notNull(id, "id");
        return getRepository().getEntityById(id);
    }

    default E getByIdOrException(Serializable id) {
        Assert.notNull(id, "id");
        return getRepository().getByIdOrException(id);
    }

    default E getByCriteria(C criteria) {
        return getRepository().getByCriteria(criteria);
    }

    default E getByCriteriaOrWarn(C criteria) {
        return getRepository().getByCriteriaOrWarn(criteria);
    }

    default E getByCriteriaOrException(C criteria) {
        return getRepository().getByCriteriaOrException(criteria);
    }

    default List<E> findByCriteria(C criteria) {
        return getRepository().findByCriteria(criteria);
    }

    default IPage<E> findPageByCriteria(C criteria) {
        return getRepository().findPageByCriteria(criteria);
    }

    default List<E> findAll() {
        return getRepository().findAll();
    }

    default Integer countByCriteria(C criteria) {
        return getRepository().countByCriteria(criteria);
    }

    default Integer countAll() {
        return getRepository().countAll();
    }

    default Boolean isPresentById(Serializable id) {
        return getRepository().isPresentById(id);
    }

    default Boolean isPresentByColumn(String propertyName, Object value) {
        return getRepository().isPresentByColumn(propertyName, value);
    }

    default Boolean isPresentByCriteria(C criteria) {
        return getRepository().isPresentByCriteria(criteria);
    }

    default Boolean isAbsentByColumn(String propertyName, Object value) {
        return !isPresentByColumn(propertyName, value);
    }

    default Boolean isAbsentByCriteria(C criteria) {
        return !isPresentByCriteria(criteria);
    }

    default void exceptionIfPresentByCriteria(C criteria) {
        getRepository().exceptionIfPresentByCriteria(criteria);
    }

    default void exceptionIfAbsentByCriteria(C criteria) {
        getRepository().exceptionIfAbsentByCriteria(criteria);
    }

    // endregion

    default void exceptionIfHaveNoCondition(C criteria) {
        if (!(criteria instanceof IWhereConditionCriteria)) {
            throw new UnsupportedOperationException("criteria不是WhereConditionCriteria，请检查代码");
        }
        if (!((IWhereConditionCriteria) criteria).hasConditions()) {
            throw new IllegalArgumentException("请至少设置1个条件");
        }
    }
}
