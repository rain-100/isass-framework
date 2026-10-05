// SPDX-License-Identifier: LGPL-3.0-only
package vip.isass.framework.entrypoint;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/** QUERY 对象扩展，输入已 URL 解码，输出由传输层统一 URL 编码。 */
public interface QueryParamConverter {
    boolean supports(Type parameterType);
    Map<String, String> toQueryParams(Object value, Type parameterType);
    Object fromQueryParams(Map<String, String> params, Type parameterType);

    static QueryParamConverter select(List<QueryParamConverter> converters, Type type) {
        QueryParamConverter selected = null;
        for (QueryParamConverter candidate : converters) {
            if (!candidate.supports(type)) continue;
            if (selected != null) throw new IllegalStateException("多个 QueryParamConverter 支持同一类型: " + type);
            selected = candidate;
        }
        return selected;
    }
}
