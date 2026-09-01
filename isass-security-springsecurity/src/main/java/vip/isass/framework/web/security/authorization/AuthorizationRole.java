// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.authorization;

/**
 * 授权上下文中的角色显示信息；编码用于程序判断，名称用于界面展示。
 */
public record AuthorizationRole(String code, String name) {
}
