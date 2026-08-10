// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.authentication.multilogin;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vip.isass.framework.common.security.AuthenticatedPrincipal;

/**
 * 是否需要下线检查器
 * 检查请求的 token 是否需要被下线
 */
@Slf4j
@Service
public class ShouldOfflineChecker {

    /**
     * 检查 token 是否需要被下线
     */
    public void checkShouldOffline(AuthenticatedPrincipal principal) {

    }

}
