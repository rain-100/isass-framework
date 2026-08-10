// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.metadata.rolecode;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import vip.isass.framework.common.web.security.metadata.rolecode.IRoleCodeService;
import vip.isass.framework.common.web.security.metadata.rolecode.RoleCodeServiceManager;

import java.util.List;

/**
 * Spring backed role code service manager.
 *
 * @author Rain
 */
@Primary
@Service
public class SpringRoleCodeServiceManager extends RoleCodeServiceManager {

    @Autowired
    public SpringRoleCodeServiceManager(List<IRoleCodeService> services) {
        super(services);
    }
}
