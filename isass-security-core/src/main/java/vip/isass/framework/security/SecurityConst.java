// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.security;

/**
 * @author Rain
 */
public interface SecurityConst {

    /**
     * 开发角色，只授权给软件开发人员，禁止授权给非开发人员
     */
    String ROLE_SUPER_DEV = "ROLE_SUPER_DEV";

    /**
     * 微服务之间调用，采用此角色
     */
    String ROLE_MS = "ROLE_MS";

    /**
     * 匿名用户角色，不需要身份验证，便可访问资源
     */
    String ROLE_ANONYMOUS = "ROLE_ANONYMOUS";

}
