// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.service;

import vip.isass.framework.common.security.AuthenticatedPrincipal;
import vip.isass.framework.common.security.CurrentPrincipalUtil;
import vip.isass.framework.common.page.Page;
import vip.isass.framework.nocode.criteria.ICriteria;
import vip.isass.framework.nocode.criteria.IUpdateCriteria;
import vip.isass.framework.nocode.criteria.field.IIdCriteria;
import vip.isass.framework.nocode.criteria.type.IOrderByCriteria;
import vip.isass.framework.nocode.criteria.type.IPageCriteria;
import vip.isass.framework.nocode.entity.CrudQueryReq;
import vip.isass.framework.nocode.entity.CursorPage;
import vip.isass.framework.nocode.entity.IIdEntity;
import vip.isass.framework.nocode.entity.ITenantEntity;
import vip.isass.framework.nocode.entity.SuperCudReq;
import vip.isass.framework.nocode.entity.SuperCudResult;
import vip.isass.framework.nocode.repository.IRepository;

import java.io.Serializable;
import java.util.Collection;

/** Local implementation contract for one NoCode CRUD aggregate. */
public interface ILocalCrudService<
        E extends IIdEntity<PK, E>,
        C extends ICriteria<E, C> & IIdCriteria<PK, E, C> & IUpdateCriteria<C>
                & IPageCriteria<E, C> & IOrderByCriteria<E, C>,
        PK extends Serializable
        > extends ICrudService<E, C, PK> {

    IRepository<E, C> getRepository();

    @Override
    @SuppressWarnings("unchecked")
    default C newCriteria() {
        try {
            var constructor = CrudServiceTypeResolver.resolveCriteriaClass(getClass()).getDeclaredConstructor();
            if (!constructor.canAccess(null)) constructor.setAccessible(true);
            return (C) constructor.newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Criteria 必须提供无参构造器: " + getClass().getName(), exception);
        }
    }

    @Override
    default SuperCudResult superCud(SuperCudReq<E, C> request) {
        return CrudWriteExecutorProvider.getRequired().superCud(this, request);
    }

    @Override
    default Page<E> page(C criteria) {
        return CrudQueryExecutorProvider.getRequired()
                .query(this, CrudQueryReq.<C, PK>page(criteria)).page();
    }

    @Override
    default CursorPage<E, PK> cursorPage(C criteria, PK cursorId, Long pageSize) {
        return CrudQueryExecutorProvider.getRequired()
                .query(this, CrudQueryReq.cursorPage(criteria, cursorId, pageSize)).cursorPage();
    }

    @Override
    default Long count(C criteria) {
        return CrudQueryExecutorProvider.getRequired()
                .query(this, CrudQueryReq.<C, PK>count(criteria)).count();
    }

    @Override
    default boolean exists(C criteria) {
        return CrudQueryExecutorProvider.getRequired()
                .query(this, CrudQueryReq.<C, PK>exists(criteria)).exists();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    default E prepareForInsert(E entity) {
        if (entity instanceof ITenantEntity tenantEntity && tenantEntity.getTenantId() == null) {
            AuthenticatedPrincipal principal = CurrentPrincipalUtil.getPrincipal();
            tenantEntity.setTenantId(principal == null || principal.getTenantId() == null
                    ? 0L : principal.getTenantId());
        }
        return entity;
    }

    default Collection<E> prepareForInsert(Collection<E> entities) {
        entities.forEach(this::prepareForInsert);
        return entities;
    }
}
