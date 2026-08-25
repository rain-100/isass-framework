// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.service;

import org.springframework.beans.factory.DisposableBean;

/** Makes the proxied executor available to generated service interface default methods. */
public final class CrudWriteExecutorProvider implements DisposableBean {

    private static volatile CrudWriteExecutor executor;
    private static final CrudWriteExecutor NON_SPRING_EXECUTOR = new CrudWriteExecutor();

    public CrudWriteExecutorProvider(CrudWriteExecutor executor) {
        CrudWriteExecutorProvider.executor = executor;
    }

    public static CrudWriteExecutor getRequired() {
        CrudWriteExecutor current = executor;
        return current == null ? NON_SPRING_EXECUTOR : current;
    }

    @Override
    public void destroy() {
        executor = null;
    }
}
