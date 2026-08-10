// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

/**
 * @author Rain
 */
@Component
public class IsassUserDetailsService implements UserDetailsService {

    @Resource
    private UserLoader userLoader;

    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        UserDetails userDetails = userLoader.load(userId);
        if (userDetails != null) {
            return userDetails;
        }
        throw new UsernameNotFoundException(userId);
    }

}
