// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.service;

import vip.isass.framework.nocode.criteria.ICriteria;
import vip.isass.framework.nocode.entity.CrudQueryReq;
import vip.isass.framework.nocode.entity.IIdEntity;
import vip.isass.framework.nocode.entity.IParentIdEntity;
import vip.isass.framework.nocode.util.TreeEntityUtil;

import java.io.Serializable;
import java.util.List;

/** Local implementation contract for the optional NoCode tree-query capability. */
public interface ILocalTreeQueryService<
        E extends IParentIdEntity<PK, E>,
        C extends ICriteria<E, C>,
        PK extends Serializable
        > extends ITreeQueryService<E, C, PK> {

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    default List<E> tree(C criteria) {
        if (!(this instanceof ILocalCrudService<?, ?, ?> localCrudService)) {
            throw new IllegalStateException("ILocalTreeQueryService 必须与 ILocalCrudService 组合使用: "
                    + getClass().getName());
        }
        CrudQueryReq request = CrudQueryReq.tree(criteria);
        return (List<E>) CrudQueryExecutorProvider.getRequired()
                .query((ILocalCrudService) localCrudService, request).tree();
    }

    @Override
    default List<PK> descendantIds(PK rootId, C criteria) {
        if (rootId == null) {
            throw new IllegalArgumentException("根节点 ID 必填");
        }
        return TreeEntityUtil.descendantIds(tree(criteria), rootId, entity -> {
            if (!(entity instanceof IIdEntity<?, ?> idEntity)) {
                throw new IllegalStateException("树查询实体必须实现 IIdEntity: " + entity.getClass().getName());
            }
            @SuppressWarnings("unchecked") PK id = (PK) idEntity.getId();
            return id;
        }, IParentIdEntity::getChildren);
    }
}
