// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.metadata;

import java.util.Collection;

/**
 * @author Rain
 */
public interface SecurityMetadataSourceProvider {

    /**
     * @param userId user id
     * @return 指定用户拥有的角色
     */
    Collection<String> findRoleCodesByUserId(String userId);

    /**
     * @param uri uri
     * @return 访问指定 uri 需要的角色
     */
    Collection<String> findRoleCodesByUri(String uri);

}
