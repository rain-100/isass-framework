// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.authorization.internal;

/** 一条内部可访问 Entrypoint 规则。 */
public record InternalAccessRule(
        String operationKey,
        String httpMethod,
        String path
) {
}
