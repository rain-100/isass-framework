// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.lifecycle;

import vip.isass.framework.nocode.entity.CrudQueryReq;
import vip.isass.framework.nocode.entity.CrudQueryResult;
import vip.isass.framework.nocode.service.CrudServiceTypeResolver;
import vip.isass.framework.nocode.service.ILocalCrudService;
import vip.isass.framework.common.criteria.WhereCondition;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 标准 NoCode 查询共用的类型化生命周期上下文。
 */
public final class CrudQueryLifecycleContext<E, C, PK extends Serializable> {

    private final ILocalCrudService<?, ?, ?> service;
    private final CrudQueryReq<C, PK> request;
    private final Map<String, Object> attributes = new LinkedHashMap<>();
    private final List<WhereCondition> readRestrictions = new ArrayList<>();
    private CrudQueryResult<E, PK> result;

    public CrudQueryLifecycleContext(ILocalCrudService<?, ?, ?> service, CrudQueryReq<C, PK> request) {
        this.service = service;
        this.request = request;
    }

    public ILocalCrudService<?, ?, ?> service() {
        return service;
    }

    public Class<?> entityClass() {
        return CrudServiceTypeResolver.resolveEntityClass(service.getClass());
    }

    public CrudQueryReq<C, PK> request() {
        return request;
    }

    public CrudQueryResult<E, PK> result() {
        return result;
    }

    public void setResult(CrudQueryResult<E, PK> result) {
        CrudQueryResult<E, PK> required = Objects.requireNonNull(result, "result");
        if (required.queryType() != request.queryType()) {
            throw new IllegalArgumentException("查询生命周期不能把 " + request.queryType()
                    + " 结果替换为 " + required.queryType());
        }
        this.result = required;
    }

    public Map<String, Object> attributes() {
        return attributes;
    }

    /**
     * 追加服务器构造的 AND 读取约束，不写入客户端 Criteria，也不授予字段输出能力。
     */
    public void addReadRestriction(WhereCondition condition) {
        readRestrictions.add(Objects.requireNonNull(condition, "服务端读取约束不能为空"));
    }

    public List<WhereCondition> readRestrictions() {
        return List.copyOf(readRestrictions);
    }
}
