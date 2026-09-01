// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.authorization.internal;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import vip.isass.framework.entrypoint.registry.ServiceDefinitionRegistry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** 当前进程内部开放 Entrypoint 的不可变注册表。 */
@Component
public final class InternalAccessRegistry {

    private final Map<String, InternalAccessRule> rules;

    public InternalAccessRegistry(List<InternalAccessProvider> providers,
                                  ServiceDefinitionRegistry serviceDefinitions) {
        LinkedHashMap<String, InternalAccessRule> collected = new LinkedHashMap<>();
        for (InternalAccessProvider provider : providers) {
            InternalAccessBuilder builder = new InternalAccessBuilder(serviceDefinitions);
            provider.defineInternalAccess(builder);
            for (InternalAccessRule rule : builder.build()) {
                String requestKey = requestKey(rule.httpMethod(), rule.path());
                InternalAccessRule duplicate = collected.putIfAbsent(requestKey, rule);
                if (duplicate != null && !duplicate.operationKey().equals(rule.operationKey())) {
                    throw new IllegalStateException("内部访问入口重复: " + requestKey);
                }
            }
        }
        this.rules = Map.copyOf(collected);
    }

    public boolean isAllowed(HttpServletRequest request) {
        return request != null && rules.containsKey(requestKey(request.getMethod(), request.getRequestURI()));
    }

    public boolean isAllowed(String method, String path) {
        return rules.containsKey(requestKey(method, path));
    }

    public List<InternalAccessRule> rules() {
        return List.copyOf(rules.values());
    }

    private String requestKey(String method, String path) {
        return (method == null ? "" : method.toUpperCase(Locale.ROOT)) + " " + path;
    }
}
