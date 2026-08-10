// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.web.security.metadata.rolecode;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class RoleCodeServiceManager implements IRoleCodeService {

    private List<IRoleCodeService> services = List.of();

    public RoleCodeServiceManager() {
    }

    public RoleCodeServiceManager(List<IRoleCodeService> services) {
        setServices(services);
    }

    public void setServices(List<IRoleCodeService> services) {
        this.services = Objects.requireNonNullElse(services, List.of());
    }

    @Override
    public Collection<String> findRoleCodesByUri(UriRoleCodesReq roleCodesReq) {
        return apply(services, s -> s.findRoleCodesByUri(roleCodesReq));
    }

    @Override
    public void setRoleCodesByUserIdCache(String userId, Collection<String> roleCodes) {
        consume(services, s -> s.setRoleCodesByUserIdCache(userId, roleCodes));
    }

    @Override
    public void setRoleCodesByUriCache(String uri, Collection<String> roleCodes) {
        consume(services, s -> s.setRoleCodesByUriCache(uri, roleCodes));
    }

    @Override
    public Collection<String> findRoleCodesByUserId(String userId) {
        return apply(services, s -> s.findRoleCodesByUserId(userId));
    }

}
