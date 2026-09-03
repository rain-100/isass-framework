// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.service;

import vip.isass.framework.common.page.Page;
import vip.isass.framework.nocode.criteria.ICriteria;
import vip.isass.framework.nocode.criteria.IUpdateCriteria;
import vip.isass.framework.nocode.criteria.field.IIdCriteria;
import vip.isass.framework.nocode.criteria.type.IOrderByCriteria;
import vip.isass.framework.nocode.criteria.type.IPageCriteria;
import vip.isass.framework.nocode.entity.CrudQueryReq;
import vip.isass.framework.nocode.entity.CrudQueryResult;
import vip.isass.framework.nocode.entity.CursorPage;
import vip.isass.framework.nocode.entity.IIdEntity;
import vip.isass.framework.nocode.lifecycle.CrudQueryLifecycleContext;
import vip.isass.framework.nocode.lifecycle.CrudQueryLifecycleListener;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Executes every standard NoCode query facade through one normalized lifecycle boundary.
 */
public final class CrudQueryExecutor {

    private static final ThreadLocal<Set<Object>> ACTIVE_SERVICES = ThreadLocal.withInitial(
            () -> Collections.newSetFromMap(new IdentityHashMap<>()));

    private final AssociationQueryCoordinator associations;
    private final List<CrudQueryLifecycleListener> listeners;

    public CrudQueryExecutor() {
        this(null, List.of());
    }

    public CrudQueryExecutor(AssociationQueryCoordinator associations,
                             List<CrudQueryLifecycleListener> listeners) {
        this.associations = associations;
        this.listeners = listeners == null ? List.of() : List.copyOf(listeners);
    }

    public <PK extends Serializable, E extends IIdEntity<PK, E>,
            C extends ICriteria<E, C> & IIdCriteria<PK, E, C> & IUpdateCriteria<C>
                    & IPageCriteria<E, C> & IOrderByCriteria<E, C>> CrudQueryResult<E, PK> query(
            ILocalCrudService<E, C, PK> service,
            CrudQueryReq<C, PK> request) {
        if (service == null) {
            throw new IllegalArgumentException("service 不能为空");
        }
        if (request == null) {
            throw new IllegalArgumentException("request 不能为空");
        }
        C criteria = request.criteria() == null ? service.newCriteria() : request.criteria().copy();
        CrudQueryReq<C, PK> normalized = new CrudQueryReq<>(
                request.queryType(), criteria, request.cursorId(), request.pageSize());

        Set<Object> active = ACTIVE_SERVICES.get();
        if (!active.add(service)) {
            return execute(service, normalized);
        }
        try {
            CrudQueryLifecycleContext<E, C, PK> context = new CrudQueryLifecycleContext<>(service, normalized);
            List<CrudQueryLifecycleListener> supported = listeners.stream()
                    .filter(listener -> listener.supports(context)).toList();
            try {
                supported.forEach(listener -> listener.beforeQuery(context));
                context.setResult(execute(service, context.request()));
                supported.forEach(listener -> listener.afterQuery(context));
                return context.result();
            } catch (RuntimeException | Error error) {
                notifyFailure(supported, context, error);
                throw error;
            }
        } finally {
            active.remove(service);
            if (active.isEmpty()) ACTIVE_SERVICES.remove();
        }
    }

    private <PK extends Serializable, E extends IIdEntity<PK, E>,
            C extends ICriteria<E, C> & IIdCriteria<PK, E, C> & IUpdateCriteria<C>
                    & IPageCriteria<E, C> & IOrderByCriteria<E, C>> CrudQueryResult<E, PK> execute(
            ILocalCrudService<E, C, PK> service,
            CrudQueryReq<C, PK> request) {
        return switch (request.queryType()) {
            case PAGE -> CrudQueryResult.page(page(service, request.criteria()));
            case CURSOR_PAGE -> CrudQueryResult.cursorPage(
                    cursorPage(service, request.criteria(), request.cursorId(), request.pageSize()));
            case COUNT -> CrudQueryResult.count(
                    service.getRepository().countByCriteria(request.criteria()).longValue());
            case EXISTS -> CrudQueryResult.exists(
                    service.getRepository().isPresentByCriteria(request.criteria()));
        };
    }

    private <PK extends Serializable, E extends IIdEntity<PK, E>,
            C extends ICriteria<E, C> & IIdCriteria<PK, E, C> & IUpdateCriteria<C>
                    & IPageCriteria<E, C> & IOrderByCriteria<E, C>> Page<E> page(
            ILocalCrudService<E, C, PK> service, C criteria) {
        Page<E> page = service.getRepository().findPageByCriteria(criteria);
        if (associations != null) {
            associations.populate(page.getRecords(), criteria);
        }
        return page;
    }

    private <PK extends Serializable, E extends IIdEntity<PK, E>,
            C extends ICriteria<E, C> & IIdCriteria<PK, E, C> & IUpdateCriteria<C>
                    & IPageCriteria<E, C> & IOrderByCriteria<E, C>> CursorPage<E, PK> cursorPage(
            ILocalCrudService<E, C, PK> service, C criteria, PK cursorId, Long pageSize) {
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
        List<E> fetched = page(service, query).getRecords();
        boolean hasMore = fetched.size() > size;
        List<E> records = hasMore ? new ArrayList<>(fetched.subList(0, (int) size)) : List.copyOf(fetched);
        PK nextCursorId = records.isEmpty() ? cursorId : records.getLast().getId();
        return new CursorPage<>(records, nextCursorId, hasMore);
    }

    private void notifyFailure(List<CrudQueryLifecycleListener> supported,
                               CrudQueryLifecycleContext<?, ?, ?> context,
                               Throwable error) {
        for (CrudQueryLifecycleListener listener : supported) {
            try {
                listener.onFailure(context, error);
            } catch (RuntimeException | Error callbackError) {
                error.addSuppressed(callbackError);
            }
        }
    }
}
