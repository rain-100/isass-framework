// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.metadata;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.web.FilterInvocation;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import vip.isass.framework.common.exception.UnifiedException;
import vip.isass.framework.common.exception.code.StatusMessageEnum;
import vip.isass.framework.web.uri.UriPrefixProvider;

import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;


/**
 * @author Rain
 */
@Slf4j
public class SecurityMetadataSource {

    private final RequestMappingHandlerMapping requestMappingHandlerMapping;
    private final SecurityMetadataSourceProviderManager securityMetadataSourceProviderManager;
    private final Collection<String> permitUrls;
    private final UriPrefixProvider uriPrefixProvider;

    private static final List<String> IGNORE_LOGGING_URI = CollUtil.newArrayList("/error");

    /**
     * Constructor for SecurityMetadataSource
     *
     * @param requestMappingHandlerMapping          request mapping handler mapping
     * @param securityMetadataSourceProviderManager security meta datasource provider manager
     * @param uriPrefixProvider                     uri prefix provider
     * @param permitUrls                            permit urls
     */
    public SecurityMetadataSource(RequestMappingHandlerMapping requestMappingHandlerMapping,
                                  SecurityMetadataSourceProviderManager securityMetadataSourceProviderManager,
                                  UriPrefixProvider uriPrefixProvider,
                                  Collection<String> permitUrls) {
        this.requestMappingHandlerMapping = requestMappingHandlerMapping;
        this.securityMetadataSourceProviderManager = securityMetadataSourceProviderManager;
        this.uriPrefixProvider = uriPrefixProvider;
        this.permitUrls = permitUrls;
    }

    public Collection<org.springframework.security.core.GrantedAuthority> getAttributes(Object object) {
        HttpServletRequest request = getRequest(object);
        if (request == null) {
            return Collections.emptyList();
        }

        Map.Entry<RequestMappingInfo, HandlerMethod> entry = getRequestMappingInfoAndHandlerMethodEntry(request);
        if (entry == null) {
            if (!permitUrls.contains(request.getRequestURI())) {
                log.trace("解析不到uri对应的方法：{}", request.getRequestURI());
            }
            return Collections.emptyList();
        }

        String httpMethod = request.getMethod();
        String mappingUri = getMappingUri(entry, object);

        Collection<String> roleCodes = securityMetadataSourceProviderManager.findRoleCodesByUri(
                httpMethod.toUpperCase() + " " + mappingUri);

        Collection<org.springframework.security.core.GrantedAuthority> attributes = new HashSet<>(16);
        if (CollUtil.isNotEmpty(roleCodes)) {
            attributes = roleCodes.stream()
                .filter(StrUtil::isNotBlank)
                .map(org.springframework.security.core.authority.SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
        }

        if (!IGNORE_LOGGING_URI.contains(request.getRequestURI())) {
            log.debug("访问 {} {} 所需权限：{}", httpMethod, request.getRequestURI(), attributes);
        }
        return attributes;
    }

    public boolean supports(Class<?> clazz) {
        return FilterInvocation.class.isAssignableFrom(clazz);
    }

    private String getMappingUri(Map.Entry<RequestMappingInfo, HandlerMethod> entry, Object object) {
        if (entry != null) {
            RequestMappingInfo info = entry.getKey();
            Set<String> patterns = null;
            if (info.getPathPatternsCondition() != null) {
                patterns = info.getPathPatternsCondition().getPatternValues();
            } else if (info.getPatternsCondition() != null) {
                patterns = info.getPatternsCondition().getPatterns();
            }

            if (patterns != null && !patterns.isEmpty()) {
                return patterns.iterator().next().trim();
            }
        }

        if (object instanceof FilterInvocation) {
            FilterInvocation invocation = (FilterInvocation) object;
            return invocation.getRequestUrl();
        }

        throw new UnifiedException(StatusMessageEnum.URI_PARSE_ERROR);
    }

    private HttpServletRequest getRequest(Object object) {
        if (object instanceof FilterInvocation) {
            FilterInvocation invocation = (FilterInvocation) object;
            return invocation.getRequest();
        }
        return null;
    }

    private Map.Entry<RequestMappingInfo, HandlerMethod> getRequestMappingInfoAndHandlerMethodEntry(HttpServletRequest request) {
        HandlerExecutionChain handlerExecutionChain;
        try {
            handlerExecutionChain = requestMappingHandlerMapping.getHandler(request);
        } catch (Exception e) {
            return null;
        }
        if (handlerExecutionChain == null) {
            return null;
        }
        Object handler = handlerExecutionChain.getHandler();
        Map<RequestMappingInfo, HandlerMethod> handlerMethods = requestMappingHandlerMapping.getHandlerMethods();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> next : handlerMethods.entrySet()) {
            HandlerMethod value = next.getValue();
            if (value.toString().equals(handler.toString())) {
                return next;
            }
        }
        return null;
    }
}
