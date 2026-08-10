// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.orm;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
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
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import vip.isass.framework.nocode.SensitiveDataProperty;
import vip.isass.framework.common.exception.AbsentException;
import vip.isass.framework.common.exception.AlreadyPresentException;
import vip.isass.framework.common.exception.code.StatusMessageEnum;
import vip.isass.framework.nocode.criteria.ICriteria;
import vip.isass.framework.nocode.criteria.type.IPageCriteria;
import vip.isass.framework.nocode.entity.IEntity;
import vip.isass.framework.nocode.entity.IIdEntity;
import vip.isass.framework.nocode.repository.IRepository;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Rain
 */
@Slf4j
public abstract class MybatisPlusRepository<
        E extends IEntity<E>,
        C extends ICriteria<E, C>,
        M extends BaseMapper<E>
        >
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

    @Override
    public boolean addIfAbsentByCriteria(E entity, C criteria) {
        if (isPresentByCriteria(criteria)) {
            return false;
        }
        add(entity);
        return true;
    }

    @Override
    public E addOrUpdate(E entity, List<String> uniqueColumns) {
        Assert.notEmpty(uniqueColumns, "uniqueColumns");
        QueryWrapper<E> wrapper = new QueryWrapper<>();
        Map<String, Object> map = BeanUtil.beanToMap(
                entity,
                new HashMap<>(16),
                true,
                key -> StrUtil.toUnderlineCase(key).toUpperCase());
        for (String uniqueColumn : uniqueColumns) {
            Object value = map.get(uniqueColumn.toUpperCase());
            if (value != null) {
                wrapper.eq(uniqueColumn, value);
            }
        }
        if (wrapper.isEmptyOfNormal()) {
            add(entity);
        } else {
            if (isPresentByWrapper(wrapper)) {
                updateByWrapper(entity, wrapper);
            } else {
                add(entity);
            }
        }
        return entity;
    }

    public boolean addIfAbsentByColumns(E entity, List<String> uniqueColumns) {
        Assert.notEmpty(uniqueColumns, "uniqueColumns");
        QueryWrapper<E> wrapper = new QueryWrapper<>();
        Map<String, Object> map = BeanUtil.beanToMap(entity);
        for (String uniqueColumn : uniqueColumns) {
            Object value = map.get(StrUtil.toCamelCase(uniqueColumn));
            Assert.notNull(value, "uniqueColumn[{}]必填", uniqueColumn);
            wrapper.eq(uniqueColumn, value);
        }

        if (isPresentByWrapper(wrapper)) {
            return false;
        }
        add(entity);
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
                return false;
            }
        }

        return super.removeByIds(realId);
    }

    public boolean deleteByWrapper(Wrapper<E> wrapper) {
        Assert.isTrue(!wrapper.isEmptyOfNormal(), "删除失败，删除条件不能为空");
        return super.remove(wrapper);
    }

    @Override
    public boolean deleteByCriteria(ICriteria<E, C> criteria) {
        return this.deleteByWrapper(WrapperUtil.getQueryWrapper(criteria));
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
                .eq(EntityPropertyColumnResolver.resolve(currentEntityClass(), "id"), idEntity.getId());

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
        return this.updateByWrapper(entity, WrapperUtil.getUpdateWrapper(criteria));
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
                    EntityPropertyColumnResolver.resolve(entityClass, "id"), realId));
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
        IPage<E> page = findPageByWrapper(new Page<E>(1, 1).setSearchCount(false), wrapper);
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
                    : CollUtil.join(((QueryWrapper<E>) wrapper).getParamNameValuePairs().values(), ",");
            throw new AbsentException(values);
        }
        return entity;
    }

    @Override
    public E getByCriteriaOrException(ICriteria<E, C> criteria) {
        return getByWrapperOrException(WrapperUtil.getQueryWrapper(criteria));
    }

    public List<E> findByWrapper(Wrapper<E> wrapper) {
        if (wrapper != null
                && !Optional.ofNullable(wrapper.getSqlSelect()).isPresent()
                && wrapper instanceof QueryWrapper) {
            ((QueryWrapper<E>) wrapper).select(
                    currentEntityClass(),
                    i -> !SensitiveDataProperty.PROPERTIES.contains(i.getProperty()));
        }
        return this.list(wrapper);
    }

    @Override
    public List<E> findByCriteria(ICriteria<E, C> criteria) {
        return this.findByWrapper(WrapperUtil.getQueryWrapper(criteria));
    }

    public IPage<E> findPageByWrapper(long pageNum, long pageSize, boolean searchCountFlag, Wrapper<E> wrapper) {
        return this.findPageByWrapper(new Page<>(pageNum, pageSize, searchCountFlag), wrapper);
    }

    public IPage<E> findPageByWrapper(IPage<E> page, Wrapper<E> wrapper) {
        if (wrapper != null
                && !Optional.ofNullable(wrapper.getSqlSelect()).isPresent()
                && wrapper instanceof QueryWrapper) {
            ((QueryWrapper<E>) wrapper).select(
                    currentEntityClass(),
                    i -> !SensitiveDataProperty.PROPERTIES.contains(i.getProperty()));
        }
        return this.page(
                        new Page<E>(page.getCurrent(), page.getSize(), page.searchCount())
                                .setOptimizeCountSql(page.optimizeCountSql()),
                        wrapper);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public IPage<E> findPageByCriteria(ICriteria<E, C> criteria) {
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
        return (int) this.count(wrapper);
    }

    @Override
    public Integer countByCriteria(ICriteria<E, C> criteria) {
        return this.countByWrapper(WrapperUtil.getQueryWrapper(criteria));
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
                .eq(EntityPropertyColumnResolver.resolve(currentEntityClass(), "id"), id)
                .last("limit 1"));
    }

    @Override
    public boolean isPresentByColumn(String propertyName, Object value) {
        Assert.notBlank(propertyName);
        Assert.notNull(value, "value");
        return isPresentByWrapper(Wrappers.<E>query()
                .eq(EntityPropertyColumnResolver.resolve(currentEntityClass(), propertyName), value));
    }

    public boolean isPresentByWrapper(Wrapper<E> wrapper) {
        return this.countByWrapper(wrapper) > 0;
    }

    @Override
    public boolean isPresentByCriteria(ICriteria<E, C> criteria) {
        return this.isPresentByWrapper(WrapperUtil.getQueryWrapper(criteria));
    }

    public void exceptionIfPresentByWrapper(Wrapper<E> wrapper) {
        if (isPresentByWrapper(wrapper)) {
            String values = wrapper == null
                    ? ""
                    : CollUtil.join(((QueryWrapper<E>) wrapper).getParamNameValuePairs().values(), ",");
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
                    : CollUtil.join(((QueryWrapper<E>) wrapper).getParamNameValuePairs().values(), ",");
            throw new AbsentException(values);
        }
    }

    @Override
    public void exceptionIfAbsentByCriteria(ICriteria<E, C> criteria) {
        exceptionIfAbsentByWrapper(WrapperUtil.getQueryWrapper(criteria));
    }

}
