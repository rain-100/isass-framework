// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.lifecycle;

import vip.isass.framework.nocode.entity.SuperCudReq;
import vip.isass.framework.nocode.entity.SuperCudResult;
import vip.isass.framework.nocode.service.CrudServiceTypeResolver;
import vip.isass.framework.nocode.service.ILocalCrudService;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Strongly typed lifecycle context for the canonical {@code superCud} write operation.
 */
public final class CrudWriteLifecycleContext<E, C> {

    private final ILocalCrudService<?, ?, ?> service;
    private final SuperCudReq<E, C> request;
    private final Map<String, Object> attributes = new LinkedHashMap<>();
    private SuperCudResult result;
    private Throwable failure;

    public CrudWriteLifecycleContext(ILocalCrudService<?, ?, ?> service, SuperCudReq<E, C> request) {
        this.service = service;
        this.request = request;
    }

    public ILocalCrudService<?, ?, ?> service() {
        return service;
    }

    public Class<?> entityClass() {
        return CrudServiceTypeResolver.resolveEntityClass(service.getClass());
    }

    public SuperCudReq<E, C> request() {
        return request;
    }

    public SuperCudResult result() {
        return result;
    }

    public void setResult(SuperCudResult result) {
        this.result = result;
    }

    public Throwable failure() {
        return failure;
    }

    public void setFailure(Throwable failure) {
        this.failure = failure;
    }

    public Map<String, Object> attributes() {
        return attributes;
    }
}
