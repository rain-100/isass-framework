// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security;

import org.springframework.security.core.userdetails.UserDetails;

/**
 * @author Administrator
 */
public interface UserLoader {

    UserDetails load(String userId);

}
