// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.entrypoint.authorization;

/**
 * URL 与 Entrypoint operation 共用的访问安全策略。
 */
public enum UrlAccessSecurityStrategy {

    /** 无需认证即可访问。 */
    NONE,

    /** 必须完成主体认证，但不要求业务权限。 */
    AUTHENTICATED,

    /** 必须完成主体认证，并通过动态权限校验。 */
    ROLE
}
