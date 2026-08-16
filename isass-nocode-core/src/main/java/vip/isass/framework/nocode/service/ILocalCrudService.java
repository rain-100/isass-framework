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
import vip.isass.framework.nocode.entity.CursorPage;
import vip.isass.framework.nocode.entity.IIdEntity;
import vip.isass.framework.nocode.entity.ITenantEntity;
import vip.isass.framework.nocode.entity.SuperCudReq;
import vip.isass.framework.nocode.entity.SuperCudResult;
import vip.isass.framework.nocode.repository.IRepository;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

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
    default SuperCudResult<E> superCud(SuperCudReq<E, C> request) {
        return CrudChangeExecutorProvider.getRequired().superCud(this, request);
    }

    @Override
    default Page<E> page(C criteria) {
        Page<E> page = getRepository().findPageByCriteria(criteria);
        AssociationQueryCoordinatorProvider.populate(page.getRecords(), criteria);
        return page;
    }

    @Override
    default CursorPage<E, PK> cursorPage(C criteria, PK cursorId, Long pageSize) {
        C query = criteria.copy();
        String orderBy = query.getOrderBy();
        String normalized = orderBy == null || orderBy.isBlank()
                ? "id asc"
                : orderBy.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
        if (!normalized.equals("id asc") && !normalized.equals("id desc")) {
            throw new IllegalArgumentException("游标分页只允许 orderBy=id asc 或 id desc");
        }
        long size = pageSize == null ? 20L : pageSize;
        if (size < 1 || size > 1000) {
            throw new IllegalArgumentException("cursorPage.pageSize 必须在 1 到 1000 之间");
        }
        if (cursorId != null) {
            if (normalized.endsWith("desc")) {
                query.setIdLessThan(cursorId);
            } else {
                query.setIdGreaterThan(cursorId);
            }
        }
        query.setOrderBy(normalized).setPageNum(1L).setPageSize(size + 1L).setSearchCountFlag(false);
        List<E> fetched = page(query).getRecords();
        boolean hasMore = fetched.size() > size;
        List<E> records = hasMore ? new ArrayList<>(fetched.subList(0, (int) size)) : List.copyOf(fetched);
        PK nextCursorId = records.isEmpty() ? cursorId : records.getLast().getId();
        return new CursorPage<>(records, nextCursorId, hasMore);
    }

    @Override
    default Long count(C criteria) {
        return getRepository().countByCriteria(criteria).longValue();
    }

    @Override
    default Boolean exists(C criteria) {
        return getRepository().isPresentByCriteria(criteria);
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
