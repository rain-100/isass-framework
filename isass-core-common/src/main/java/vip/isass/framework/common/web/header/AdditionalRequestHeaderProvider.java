// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.web.header;

import java.net.URI;
import java.util.Map;

/**
 * 附加请求头提供者接口
 *
 * @author Rain
 */
public interface AdditionalRequestHeaderProvider {

    String getHeaderName();

    String getValue();

    /**
     * 当已存在同名的请求头时，是否覆盖旧的值
     *
     * @return is override
     */
    boolean override();

    boolean support(String method, String uri);

    /**
     * 返回本次请求需要附加的全部请求头。旧实现仍可只实现单请求头方法。
     */
    default Map<String, String> getHeaders(AdditionalRequestHeaderContext context) {
        String uri = context.uri() == null ? null : context.uri().toString();
        if (!support(context.method(), uri)) {
            return Map.of();
        }
        String value = getValue();
        return value == null ? Map.of() : Map.of(getHeaderName(), value);
    }

}
