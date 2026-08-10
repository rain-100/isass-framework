// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.config;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import vip.isass.framework.web.interceptor.IsassHandlerInterceptor;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * @author Rain
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final List<IsassHandlerInterceptor> isassHandlerInterceptors;

    @Getter
    private final String allowedOrigins;

    @Getter
    private final String allowedMethods;

    @Getter
    private final String allowedHeaders;

    public WebConfig(List<IsassHandlerInterceptor> isassHandlerInterceptors,
                     @Value("${isass.web.cors.allowed-origins:*}") String allowedOrigins,
                     @Value("${isass.web.cors.allowed-methods:*}") String allowedMethods,
                     @Value("${isass.web.cors.allowed-headers:*}") String allowedHeaders) {
        this.isassHandlerInterceptors = isassHandlerInterceptors;
        this.allowedOrigins = allowedOrigins;
        this.allowedMethods = allowedMethods;
        this.allowedHeaders = allowedHeaders;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        for (IsassHandlerInterceptor interceptor : isassHandlerInterceptors) {
            registry.addInterceptor(interceptor)
                .addPathPatterns(interceptor.getPatterns() == null
                    ? Collections.singletonList("/**")
                    : interceptor.getPatterns());
        }
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                    .allowedOrigins(allowedOrigins)
                    .allowedMethods(allowedMethods)
                    .allowedHeaders(allowedHeaders);
            }
        };
    }

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        try {
            org.springframework.core.io.Resource[] resources = ResourcePatternUtils.getResourcePatternResolver(null)
                .getResources("classpath*:META-INF/mime.type");
            for (org.springframework.core.io.Resource resource : resources) {
                if (!resource.exists()) {
                    continue;
                }
                List<String> mimeTypes = FileUtil.readUtf8Lines(resource.getURL());
                for (String line : mimeTypes) {
                    line = StrUtil.replace(line, StrUtil.TAB, StrUtil.SPACE).trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    String[] split = line.split(StrUtil.SPACE);
                    if (split.length == 1) {
                        continue;
                    }
                    MediaType mediaType = MediaType.valueOf(split[0]);
                    for (int i = 1; i < split.length; i++) {
                        if (split[i].isEmpty()) {
                            continue;
                        }
                        configurer.mediaType(split[i], mediaType);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("can not read file META-INF/mime.type: " + e);
        }
    }
}
