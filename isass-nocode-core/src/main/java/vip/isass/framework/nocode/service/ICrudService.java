// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.service;

import vip.isass.framework.common.exception.AbsentException;
import vip.isass.framework.common.exception.AlreadyPresentException;
import vip.isass.framework.common.page.Page;
import vip.isass.framework.entrypoint.IEntrypoint;
import vip.isass.framework.entrypoint.annotation.BodyParam;
import vip.isass.framework.entrypoint.annotation.EntrypointOperation;
import vip.isass.framework.entrypoint.annotation.QueryParam;
import vip.isass.framework.entrypoint.metadata.HttpMethod;
import vip.isass.framework.nocode.criteria.ICriteria;
import vip.isass.framework.nocode.criteria.IUpdateCriteria;
import vip.isass.framework.nocode.criteria.NullValueMode;
import vip.isass.framework.nocode.criteria.field.IIdCriteria;
import vip.isass.framework.nocode.criteria.type.IOrderByCriteria;
import vip.isass.framework.nocode.criteria.type.IPageCriteria;
import vip.isass.framework.nocode.entity.CreateIfAbsentResult;
import vip.isass.framework.nocode.entity.CursorPage;
import vip.isass.framework.nocode.entity.IIdEntity;
import vip.isass.framework.nocode.entity.SuperCudReq;
import vip.isass.framework.nocode.entity.SuperCudResult;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/** NoCode standard CRUD application entrypoint. */
public interface ICrudService<
        E extends IIdEntity<PK, E>,
        C extends ICriteria<E, C>
                & IIdCriteria<PK, E, C>
                & IUpdateCriteria<C>
                & IPageCriteria<E, C>
                & IOrderByCriteria<E, C>,
        PK extends Serializable
        > extends IEntrypoint {

    C newCriteria();

    default E create(E entity) {
        return superCud(SuperCudReq.<E, C>add(entity)).addEntities().getFirst();
    }

    @EntrypointOperation(operationName = "createBatch", displayName = "增-批量",
            description = "批量新增数据", displayOrder = 101, httpMethod = HttpMethod.POST)
    default List<E> createBatch(@BodyParam Collection<E> entities) {
        if (entities == null || entities.isEmpty()) {
            throw new IllegalArgumentException("createBatch.entities 不能为空");
        }
        return superCud(SuperCudReq.<E, C>addAll(entities)).addEntities();
    }

    default CreateIfAbsentResult<E> createIfAbsent(E entity, C criteria) {
        return superCud(SuperCudReq.<E, C>addIfAbsent(entity, criteria))
                .addIfAbsentResults().getFirst();
    }

    default boolean update(E entity) {
        return superCud(SuperCudReq.<E, C>update(entity)).updateEntities().size() == 1;
    }

    default int update(E entity, C criteria) {
        return updateBatch(List.of(entity), criteria);
    }

    @EntrypointOperation(operationName = "updateBatch", displayName = "改-批量",
            description = "根据实体 ID 或查询条件批量修改数据", displayOrder = 201,
            httpMethod = HttpMethod.PUT)
    default int updateBatch(@BodyParam Collection<E> entities, @QueryParam C criteria) {
        if (entities == null || entities.isEmpty()) {
            throw new IllegalArgumentException("updateBatch.entities 不能为空");
        }
        return superCud(SuperCudReq.<E, C>updateByCriteria(entities, criteria))
                .updateByCriteriaCounts().getFirst();
    }

    default int requireUpdate(E entity, C criteria) {
        int affected = update(entity, criteria);
        if (affected == 0) {
            throw new AbsentException("没有符合更新条件的记录");
        }
        return affected;
    }

    default boolean delete(PK id) {
        return superCud(SuperCudReq.<E, C>delete(id)).deleteIds().size() == 1;
    }

    default boolean deleteIds(Collection<PK> ids) {
        return deleteBatch(newCriteria().setIdIn(ids)) > 0;
    }

    default int updateAllColumns(E entity) {
        entity.markAllPresentProperties();
        return update(entity, newCriteria().setNullValueMode(NullValueMode.WRITE_NULL));
    }

    @EntrypointOperation(operationName = "deleteBatch", displayName = "删-批量",
            description = "根据查询条件批量删除数据", displayOrder = 401,
            httpMethod = HttpMethod.DELETE)
    default int deleteBatch(@QueryParam C criteria) {
        return superCud(SuperCudReq.<E, C>deleteByCriteria(criteria))
                .deleteByCriteriaCounts().getFirst();
    }

    @EntrypointOperation(operationName = "superCud", displayName = "超级增删改",
            description = "在一个事务中执行多种新增、修改和删除操作", displayOrder = 202,
            httpMethod = HttpMethod.POST)
    SuperCudResult<E> superCud(@BodyParam SuperCudReq<E, C> request);

    @EntrypointOperation(operationName = "page", displayName = "查-分页列表",
            description = "根据查询条件返回分页列表", displayOrder = 301,
            httpMethod = HttpMethod.GET)
    Page<E> page(@QueryParam C criteria);

    @EntrypointOperation(operationName = "cursorPage", displayName = "查-游标分页",
            description = "按 ID 指定方向返回下一页", displayOrder = 302,
            httpMethod = HttpMethod.GET)
    CursorPage<E, PK> cursorPage(@QueryParam C criteria,
                                 @QueryParam("cursorId") PK cursorId,
                                 @QueryParam("pageSize") Long pageSize);

    @EntrypointOperation(operationName = "count", displayName = "查-数量",
            description = "根据查询条件统计数据数量", displayOrder = 303,
            httpMethod = HttpMethod.GET)
    Long count(@QueryParam C criteria);

    @EntrypointOperation(operationName = "exists", displayName = "查-是否存在",
            description = "判断是否存在符合查询条件的数据", displayOrder = 304,
            httpMethod = HttpMethod.GET)
    Boolean exists(@QueryParam C criteria);

    default E getById(PK id) {
        return getOne(newCriteria().setId(id));
    }

    default E getOne(C criteria) {
        C query = criteria.copy().setPageNum(1L).setPageSize(1L).setSearchCountFlag(false);
        List<E> records = page(query).getRecords();
        return records.isEmpty() ? null : records.getFirst();
    }

    default E requireOne(C criteria) {
        E entity = getOne(criteria);
        if (entity == null) {
            throw new AbsentException("记录不存在");
        }
        return entity;
    }

    default List<E> list(C criteria) {
        return page(criteria.copy().setPageNum(1L).setPageSize(9999L).setSearchCountFlag(false)).getRecords();
    }

    default Long countAll() {
        return count(newCriteria());
    }

    default Boolean isPresentById(PK id) {
        return exists(newCriteria().setId(id));
    }

    default boolean absent(C criteria) {
        return !exists(criteria);
    }

    default void requireExists(C criteria) {
        if (!exists(criteria)) {
            throw new AbsentException("记录不存在");
        }
    }

    default void requireAbsent(C criteria) {
        if (exists(criteria)) {
            throw new AlreadyPresentException("记录已存在");
        }
    }
}
