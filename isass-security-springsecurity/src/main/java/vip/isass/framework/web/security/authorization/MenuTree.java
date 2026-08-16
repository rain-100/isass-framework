// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.authorization;

import java.util.List;

/** Cross-service navigation menu tree node. */
public record MenuTree(
        Long id,
        String name,
        String uri,
        Integer type,
        Integer orderNum,
        List<MenuTree> children
) {
    public MenuTree {
        children = children == null ? List.of() : List.copyOf(children);
    }
}
