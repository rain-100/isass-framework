// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.entrypoint.authorization;

import java.util.Collection;

/**
 * 当前进程的 Entrypoint 操作与权限编码映射。
 */
public interface EntrypointPermissionResolver {

    Collection<String> findRequiredPermissionCodes(String httpMethod, String requestPath);
}
