// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.service;

import org.springframework.beans.factory.DisposableBean;

/** Makes the Spring-owned canonical query executor available to generated service defaults. */
public final class CrudQueryExecutorProvider implements DisposableBean {

    private static final CrudQueryExecutor NON_SPRING_EXECUTOR = new CrudQueryExecutor();
    private static volatile CrudQueryExecutor executor;

    public CrudQueryExecutorProvider(CrudQueryExecutor executor) {
        CrudQueryExecutorProvider.executor = executor;
    }

    public static CrudQueryExecutor getRequired() {
        CrudQueryExecutor current = executor;
        return current == null ? NON_SPRING_EXECUTOR : current;
    }

    @Override
    public void destroy() {
        executor = null;
    }
}
