// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.database.mybatisplus.orm;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.toolkit.reflect.GenericTypeUtils;
import com.baomidou.mybatisplus.spring.service.IService;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.github.yulichang.base.MPJBaseMapper;
import com.github.yulichang.wrapper.DeleteJoinWrapper;
import com.github.yulichang.wrapper.UpdateJoinWrapper;
import lombok.extern.slf4j.Slf4j;
import vip.isass.framework.common.exception.AbsentException;
import vip.isass.framework.common.exception.AlreadyPresentException;
import vip.isass.framework.common.exception.code.StatusMessageEnum;
import vip.isass.framework.common.page.Page;
import vip.isass.framework.common.criteria.ICriteria;
import vip.isass.framework.common.criteria.IUpdateCriteria;
import vip.isass.framework.common.criteria.NullValueMode;
import vip.isass.framework.common.criteria.type.IOrderByCriteria;
import vip.isass.framework.common.criteria.type.IPageCriteria;
import vip.isass.framework.common.entity.IEntity;
import vip.isass.framework.common.entity.IIdEntity;
import vip.isass.framework.database.core.repository.IRepository;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Rain
 */
@Slf4j
public abstract class MybatisPlusRepository<E extends IEntity<E>, C extends ICriteria<E, C>, M extends MPJBaseMapper<E>>
        extends ServiceImpl<M, E>
        implements IRepository<E, C> {

    @SuppressWarnings("unchecked")
    protected Class<E> currentEntityClass() {
        return (Class<E>) GenericTypeUtils.resolveTypeArguments(getClass(), MybatisPlusRepository.class)[0];
    }

    // ****************************** 增 start ******************************
    @Override
    public boolean add(E entity) {
        super.save(entity);
        return true;
    }

    @Override
    public boolean addBatch(Collection<E> entities) {
        return addBatch(entities, IService.DEFAULT_BATCH_SIZE);
    }

    @Override
    public boolean addBatch(Collection<E> entities, int batchSize) {
        if (CollUtil.isEmpty(entities)) {
            return false;
        }
        super.saveBatch(entities, batchSize);
        return true;
    }

    // ****************************** 删 start ******************************

    @Override
    public boolean deleteById(Serializable id) {
        Class<E> entityClass = currentEntityClass();
        Serializable realId = id;
        TableInfo tableInfo = TableInfoHelper.getTableInfo(entityClass);
        if (tableInfo != null && Number.class.isAssignableFrom(tableInfo.getKeyType())) {
            try {
                realId = Long.parseLong(id.toString());
            } catch (NumberFormatException e) {
                log.error(e.getMessage(), e);
                return false;
            }
        }

        return super.removeById(realId);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public boolean deleteByIds(Collection<? extends Serializable> ids) {
        return deleteCountByIds(ids) > 0;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public int deleteCountByIds(Collection<? extends Serializable> ids) {
        Class<E> entityClass = currentEntityClass();
        Collection realId = ids;
        TableInfo tableInfo = TableInfoHelper.getTableInfo(entityClass);
        if (tableInfo != null && Number.class.isAssignableFrom(tableInfo.getKeyType())) {
            try {
                if (!(CollUtil.getFirst(ids) instanceof Number)) {
                    realId = new ArrayList<>(ids.size());
                    for (Serializable id : ids) {
                        Long l = Long.parseLong(id.toString());
                        realId.add(l);
                    }
                }

            } catch (NumberFormatException e) {
                log.error(e.getMessage(), e);
                return 0;
            }
        }

        return getBaseMapper().deleteByIds(realId);
    }

    public boolean deleteByWrapper(Wrapper<E> wrapper) {
        Assert.isTrue(!wrapper.isEmptyOfNormal(), "删除失败，删除条件不能为空");
        return super.remove(wrapper);
    }

    @Override
    public boolean deleteByCriteria(ICriteria<E, C> criteria) {
        return deleteCountByCriteria(criteria) > 0;
    }

    @Override
    public int deleteCountByCriteria(ICriteria<E, C> criteria) {
        if (WrapperUtil.hasRelationalFilter(criteria)) {
            DeleteJoinWrapper<E> join = WrapperUtil.getJoinDeleteWrapper(criteria);
            return mpjMapper().deleteJoin(join);
        }
        Wrapper<E> wrapper = WrapperUtil.getDeleteWrapper(criteria);
        Assert.isTrue(!wrapper.isEmptyOfNormal(), "删除失败，删除条件不能为空");
        return getBaseMapper().delete(wrapper);
    }

    //****************************** 改 start ******************************

    @Override
    @SuppressWarnings("rawtypes")
    public boolean updateById(E entity) {
        IIdEntity idEntity = (IIdEntity) entity;
        Serializable id = idEntity.getId();
        Assert.notNull(id, "id 不能为null");
        if (id instanceof String) {
            Assert.notBlank((String) id, "id 不能为空");
        }
        return super.updateById(entity);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public boolean updateAllColumnsById(E entity) {
        IIdEntity idEntity = (IIdEntity) entity;
        Serializable id = idEntity.getId();
        Assert.notNull(id, "id 不能为null");
        if (id instanceof String) {
            Assert.notBlank((String) id, "id 不能为空");
        }

        UpdateWrapper<E> updateWrapper = new UpdateWrapper<E>()
                .eq(WrapperUtil.resolveColumnName(currentEntityClass(), "id"), idEntity.getId());

        Class<E> entityClass = currentEntityClass();
        TableInfo tableInfo = TableInfoHelper.getTableInfo(entityClass);
        Map<String, Object> map = BeanUtil.beanToMap(entity);
        for (TableFieldInfo tableFieldInfo : tableInfo.getFieldList()) {
            Object value = map.get(tableFieldInfo.getProperty());
            if (value != null) {
                continue;
            }

            FieldFill fieldFill = tableFieldInfo.getFieldFill();
            if (fieldFill != FieldFill.DEFAULT) {
                continue;
            }
            updateWrapper.set(tableFieldInfo.getColumn(), null);
        }

        return super.update(entity, updateWrapper);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public boolean updateByWrapper(E entity, Wrapper<E> wrapper) {
        Assert.isTrue(!wrapper.isEmptyOfNormal(), "更新失败，更新条件不能为空");
        return this.update(entity, wrapper);
    }

    @Override
    public boolean updateByCriteria(E entity, ICriteria<E, C> criteria) {
        return updateCountByCriteria(entity, criteria) > 0;
    }

    @Override
    public int updateCountByCriteria(E entity, ICriteria<E, C> criteria) {
        Assert.notNull(entity, "更新实体不能为空");
        Map<String, Object> values = BeanUtil.beanToMap(entity);
        List<TableFieldInfo> businessFields = WrapperUtil.resolveWritableBusinessColumns(currentEntityClass());
        if (businessFields.isEmpty()) throw new IllegalArgumentException("实体没有可更新的业务字段");
        boolean writeNull = criteria instanceof IUpdateCriteria<?> updateCriteria
                && updateCriteria.resolveNullValueMode() == NullValueMode.WRITE_NULL;
        boolean tracked = !entity.presentProperties().isEmpty();
        boolean rootSubmitted = businessFields.stream().anyMatch(field -> writeNull && tracked
                ? entity.isPropertyPresent(field.getProperty()) : values.get(field.getProperty()) != null);
        if (writeNull && !rootSubmitted) {
            boolean associationSubmitted = entity.associations().stream().anyMatch(association ->
                    tracked && entity.isPropertyPresent(association.property())
                            && values.get(association.property()) != null);
            rootSubmitted = !associationSubmitted;
        }
        if (!rootSubmitted) throw new IllegalArgumentException("更新没有可写的主实体业务字段");
        E writableEntity = writableEntity(values, businessFields);
        boolean relational = WrapperUtil.hasRelationalFilter(criteria);
        if (relational) {
            UpdateJoinWrapper<E> join = WrapperUtil.getJoinUpdateWrapper(criteria);
            if (writeNull) {
                for (TableFieldInfo field : businessFields) {
                    if (values.get(field.getProperty()) == null) {
                        join.setSql(true, join.getAlias() + "." + field.getColumn() + " = NULL");
                    }
                }
            }
            return mpjMapper().updateJoin(writableEntity, join);
        }
        UpdateWrapper<E> wrapper = WrapperUtil.getUpdateWrapper(criteria);
        Assert.isTrue(!wrapper.isEmptyOfNormal(), "更新失败，更新条件不能为空");
        if (writeNull) {
            for (TableFieldInfo field : businessFields) {
                if (values.get(field.getProperty()) == null) {
                    wrapper.set(field.getColumn(), null);
                }
            }
        }
        return getBaseMapper().update(writableEntity, wrapper);
    }

    private E writableEntity(Map<String, Object> values, List<TableFieldInfo> businessFields) {
        Map<String, Object> writable = new HashMap<>();
        for (TableFieldInfo field : businessFields) {
            Object value = values.get(field.getProperty());
            if (value != null) writable.put(field.getProperty(), value);
        }
        for (TableFieldInfo field : TableInfoHelper.getTableInfo(currentEntityClass()).getFieldList()) {
            if (field.isVersion() && values.get(field.getProperty()) != null) {
                writable.put(field.getProperty(), values.get(field.getProperty()));
            }
        }
        return BeanUtil.toBean(writable, currentEntityClass());
    }

    // ****************************** 查 start ******************************

    @Override
    public E getEntityById(Serializable id) {
        Class<E> entityClass = currentEntityClass();
        Serializable realId = id;
        TableInfo tableInfo = TableInfoHelper.getTableInfo(entityClass);
        if (tableInfo != null && Number.class.isAssignableFrom(tableInfo.getKeyType())) {
            try {
                realId = Long.parseLong(id.toString());
            } catch (NumberFormatException e) {
                log.error(e.getMessage(), e);
                return null;
            }
        }

        if (IIdEntity.class.isAssignableFrom(entityClass)) {
            return getByWrapper(new QueryWrapper<E>().eq(
                    WrapperUtil.resolveColumnName(entityClass, "id"), realId));
        }
        return super.getById(realId);
    }

    @Override
    public E getByIdOrException(Serializable id) {
        E t = this.getEntityById(id);
        if (t == null) {
            throw new AbsentException(id.toString());
        }
        return t;
    }

    public E getByWrapper(Wrapper<E> wrapper) {
        IPage<E> page = findMybatisPlusPage(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<E>(1, 1)
                        .setSearchCount(false), wrapper);
        return page.getRecords().isEmpty() ? null : page.getRecords().get(0);
    }

    @Override
    public E getByCriteria(ICriteria<E, C> criteria) {
        return getByWrapper(WrapperUtil.getQueryWrapper(criteria));
    }

    public E getOrWarnByWrapper(Wrapper<E> wrapper) {
        E t = getByWrapper(wrapper);
        if (t == null) {
            log.warn(
                    "{}: {}: {}",
                    StatusMessageEnum.ABSENT.getMsg(),
                    currentEntityClass().getSimpleName(),
                    wrapper.getSqlSegment());
        }
        return t;
    }

    @Override
    public E getByCriteriaOrWarn(ICriteria<E, C> criteria) {
        return getOrWarnByWrapper(WrapperUtil.getQueryWrapper(criteria));
    }

    public E getByWrapperOrException(Wrapper<E> wrapper) {
        E entity = getByWrapper(wrapper);
        if (entity == null) {
            String values = wrapper == null
                    ? ""
                    : wrapper.getSqlSegment();
            throw new AbsentException(values);
        }
        return entity;
    }

    @Override
    public E getByCriteriaOrException(ICriteria<E, C> criteria) {
        return getByWrapperOrException(WrapperUtil.getQueryWrapper(criteria));
    }

    public List<E> findByWrapper(Wrapper<E> wrapper) {
        if (wrapper instanceof MpjWrapper<E> join) {
            return mpjMapper().selectJoinList(currentEntityClass(), join);
        }
        WrapperUtil.applyDefaultSelectColumns(wrapper, currentEntityClass());
        return this.list(wrapper);
    }

    @Override
    public List<E> findByCriteria(ICriteria<E, C> criteria) {
        return this.findByWrapper(WrapperUtil.getQueryWrapper(criteria));
    }

    public Page<E> findPageByWrapper(long pageNum, long pageSize, boolean searchCountFlag,
                                           Wrapper<E> wrapper) {
        IPage<E> page = findMybatisPlusPage(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(
                        pageNum, pageSize, searchCountFlag), wrapper);
        return Page.of(page.getRecords(), page.getCurrent(), page.getSize(), page.getTotal());
    }

    private IPage<E> findMybatisPlusPage(IPage<E> page, Wrapper<E> wrapper) {
        if (wrapper instanceof MpjWrapper<E> join) {
            return mpjMapper().selectJoinPage(page, currentEntityClass(), join);
        }
        WrapperUtil.applyDefaultSelectColumns(wrapper, currentEntityClass());
        return this.page(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<E>(
                        page.getCurrent(), page.getSize(), page.searchCount())
                        .setOptimizeCountSql(page.optimizeCountSql()),
                wrapper);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Page<E> findPageByCriteria(ICriteria<E, C> criteria) {
        IPageCriteria pageCriteria = (IPageCriteria) criteria;
        return findPageByWrapper(
                pageCriteria.getPageNum(),
                pageCriteria.getPageSize(),
                pageCriteria.getSearchCountFlag(),
                WrapperUtil.getQueryWrapper(criteria));
    }

    @Override
    public List<E> findAll() {
        return this.findByWrapper(null);
    }

    public Integer countByWrapper(Wrapper<E> wrapper) {
        if (wrapper instanceof MpjWrapper<E> join) {
            // 分页插件按 Wrapper 的实际 JOIN 结构选择普通或根实体计数，零大小页不读取实体。
            IPage<E> count = mpjMapper().selectJoinPage(
                    new com.baomidou.mybatisplus.extension.plugins.pagination.Page<E>(1, 0, true),
                    currentEntityClass(), join);
            return Math.toIntExact(count.getTotal());
        }
        return (int) this.count(wrapper);
    }

    @Override
    public Integer countByCriteria(ICriteria<E, C> criteria) {
        return this.countByWrapper(WrapperUtil.getCountQueryWrapper(criteria));
    }

    @Override
    public Integer countAll() {
        return (int) this.count(null);
    }

    @Override
    public boolean isPresentById(Serializable id) {
        Assert.notNull(id, "id");
        if (id instanceof String) {
            Assert.notBlank((String) id, "id");
        }

        return isPresentByWrapper(Wrappers.<E>query()
                .select(WrapperUtil.resolveColumnName(currentEntityClass(), "id"))
                .eq(WrapperUtil.resolveColumnName(currentEntityClass(), "id"), id));
    }

    @Override
    public boolean isPresentByColumn(String propertyName, Object value) {
        Assert.notBlank(propertyName);
        Assert.notNull(value, "value");
        return isPresentByWrapper(Wrappers.<E>query()
                .eq(WrapperUtil.resolveColumnName(currentEntityClass(), propertyName), value));
    }

    public boolean isPresentByWrapper(Wrapper<E> wrapper) {
        IPage<E> page = findMybatisPlusPage(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<E>(1, 1)
                        .setSearchCount(false), wrapper);
        return !page.getRecords().isEmpty();
    }

    @Override
    public boolean isPresentByCriteria(ICriteria<E, C> criteria) {
        ICriteria<E, C> query = criteria;
        if (query instanceof IOrderByCriteria<?, ?>) {
            query = criteria.copy();
            ((IOrderByCriteria<?, ?>) query).setOrderBy(null);
        }
        Wrapper<E> wrapper = WrapperUtil.getCountQueryWrapper(query);
        if (wrapper instanceof MpjWrapper<E> queryWrapper && queryWrapper.getFrom().isBlank()
                && IIdEntity.class.isAssignableFrom(currentEntityClass())) {
            queryWrapper.getSelectColumns().clear();
            queryWrapper.select(BaseColumnFactory.column(queryWrapper.getBaseColumn(), "id"));
        }
        return isPresentByWrapper(wrapper);
    }

    public void exceptionIfPresentByWrapper(Wrapper<E> wrapper) {
        if (isPresentByWrapper(wrapper)) {
            String values = wrapper == null
                    ? ""
                    : wrapper.getSqlSegment();
            throw new AlreadyPresentException(values);
        }
    }

    @Override
    public void exceptionIfPresentByCriteria(ICriteria<E, C> criteria) {
        exceptionIfPresentByWrapper(WrapperUtil.getQueryWrapper(criteria));
    }

    public void exceptionIfAbsentByWrapper(Wrapper<E> wrapper) {
        if (!isPresentByWrapper(wrapper)) {
            String values = wrapper == null
                    ? ""
                    : wrapper.getSqlSegment();
            throw new AbsentException(values);
        }
    }

    @Override
    public void exceptionIfAbsentByCriteria(ICriteria<E, C> criteria) {
        exceptionIfAbsentByWrapper(WrapperUtil.getQueryWrapper(criteria));
    }

    private MPJBaseMapper<E> mpjMapper() {
        return getBaseMapper();
    }

}
