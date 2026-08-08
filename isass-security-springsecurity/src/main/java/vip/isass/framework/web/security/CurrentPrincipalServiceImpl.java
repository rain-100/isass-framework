package vip.isass.framework.web.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import vip.isass.framework.common.security.AuthenticatedPrincipal;
import vip.isass.framework.common.security.CurrentPrincipalService;
import vip.isass.framework.web.security.authentication.PrincipalAuthenticationToken;

/** Reads the authenticated principal from Spring Security's request context. */
@Service
public class CurrentPrincipalServiceImpl implements CurrentPrincipalService {

    @Override
    public AuthenticatedPrincipal getPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication instanceof PrincipalAuthenticationToken token ? token.getPrincipal() : null;
    }
}
