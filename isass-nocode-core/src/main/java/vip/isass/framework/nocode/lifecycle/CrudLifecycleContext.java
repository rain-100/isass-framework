// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.lifecycle;

import vip.isass.framework.nocode.service.ILocalService;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/** Mutable operation context shared by CRUD lifecycle listeners. */
public final class CrudLifecycleContext {

    private final ILocalService<?, ?> service;
    private final CrudOperation operation;
    private final String methodName;
    private final Object[] arguments;
    private final Map<String, Object> attributes = new LinkedHashMap<>();
    private Object result;

    public CrudLifecycleContext(ILocalService<?, ?> service, CrudOperation operation,
                                String methodName, Object[] arguments) {
        this.service = service;
        this.operation = operation;
        this.methodName = methodName;
        this.arguments = arguments == null ? new Object[0] : Arrays.copyOf(arguments, arguments.length);
    }

    public ILocalService<?, ?> service() {
        return service;
    }

    public Class<?> entityClass() {
        return service.entityClass();
    }

    public CrudOperation operation() {
        return operation;
    }

    public String methodName() {
        return methodName;
    }

    public Object[] arguments() {
        return Arrays.copyOf(arguments, arguments.length);
    }

    public Object result() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public Map<String, Object> attributes() {
        return attributes;
    }
}
