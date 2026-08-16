// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.entrypoint.registry;

import vip.isass.framework.entrypoint.IEntrypoint;

import java.lang.reflect.Method;

/** Optional classifier contributed by higher-level modules such as NoCode. */
public interface EntrypointClassifier {

    boolean isNocode(Class<? extends IEntrypoint> serviceInterface, Method operationMethod);
}
