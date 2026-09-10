// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.service;

import vip.isass.framework.entrypoint.IEntrypoint;
import vip.isass.framework.entrypoint.annotation.EntrypointOperation;
import vip.isass.framework.entrypoint.annotation.QueryParam;
import vip.isass.framework.entrypoint.metadata.HttpMethod;
import vip.isass.framework.nocode.criteria.ICriteria;
import vip.isass.framework.nocode.entity.IParentIdEntity;

import java.io.Serializable;
import java.util.List;

/** Optional NoCode query capability for entities with parent/children projections. */
public interface ITreeQueryService<
        E extends IParentIdEntity<PK, E>,
        C extends ICriteria<E, C>,
        PK extends Serializable
        > extends IEntrypoint {

    @EntrypointOperation(operationName = "tree", displayName = "查询树",
            description = "根据查询条件返回完整树形数据", displayOrder = 105,
            httpMethod = HttpMethod.GET)
    List<E> tree(@QueryParam C criteria);

    @EntrypointOperation(operationName = "descendantIds", displayName = "查询后代节点 ID",
            description = "根据查询条件查询指定节点的全部后代节点 ID，不包含指定节点本身", displayOrder = 106,
            httpMethod = HttpMethod.GET)
    List<PK> descendantIds(@QueryParam("rootId") PK rootId, @QueryParam C criteria);
}
