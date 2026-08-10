// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * @author Rain
 */
@Component
public class IsassUserLoader implements UserLoader {

    @Override
    public UserDetails load(String userId) {
        return null;
        // return new User(userId, "{noop}123456", AuthorityUtils.createAuthorityList("admin"));
    }

}
