// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.interceptor;

import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

/**
 * isass HandlerInterceptor
 *
 * @author : rain
 * @since : 2022/11/23
 */
public interface IsassHandlerInterceptor extends HandlerInterceptor {

    /**
     * 默认 pattern 为 /**
     *
     * @return patterns
     */
    default List<String> getPatterns() {
        return null;
    }

}
