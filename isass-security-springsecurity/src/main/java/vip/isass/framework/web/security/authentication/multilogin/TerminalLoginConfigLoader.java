// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.authentication.multilogin;

import java.util.List;

/**
 * 多端登录配置加载器
 *
 * @author Rain
 */
public interface TerminalLoginConfigLoader {

    List<TerminalLoginConfig> load();

}
