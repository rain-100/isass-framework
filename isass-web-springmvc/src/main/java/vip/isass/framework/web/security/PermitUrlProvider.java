// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security;

import java.util.Collection;

/**
 * 提供允许访问的 url 集合
 * 服务启动时，框架会调用 getUrls() 方法，把 urls 配置到 spring security url 放行列表
 *
 * @author Rain
 */
public interface PermitUrlProvider {

    Collection<String> getUrls();

}
