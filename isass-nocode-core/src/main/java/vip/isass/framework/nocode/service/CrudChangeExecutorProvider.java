// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.service;

import org.springframework.beans.factory.DisposableBean;

/** Makes the proxied executor available to generated service interface default methods. */
public final class CrudChangeExecutorProvider implements DisposableBean {

    private static volatile CrudChangeExecutor executor;
    private static final CrudChangeExecutor NON_SPRING_EXECUTOR = new CrudChangeExecutor();

    public CrudChangeExecutorProvider(CrudChangeExecutor executor) {
        CrudChangeExecutorProvider.executor = executor;
    }

    public static CrudChangeExecutor getRequired() {
        CrudChangeExecutor current = executor;
        return current == null ? NON_SPRING_EXECUTOR : current;
    }

    @Override
    public void destroy() {
        executor = null;
    }
}
