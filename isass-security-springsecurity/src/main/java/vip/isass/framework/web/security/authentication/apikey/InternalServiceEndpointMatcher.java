// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.authentication.apikey;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/** Matches outbound URLs that belong to configured or discovered ISASS microservices. */
final class InternalServiceEndpointMatcher {

    private static final String ENDPOINT_PREFIX = "isass.entrypoint.http.services.";
    private static final String ENDPOINT_SUFFIX = ".url";
    private static final String BASE_URL = "isass.entrypoint.http.base-url";
    private static final String DISCOVERY_CLIENT = "org.springframework.cloud.client.discovery.DiscoveryClient";

    private final Environment environment;
    private final ListableBeanFactory beanFactory;

    InternalServiceEndpointMatcher(Environment environment, ListableBeanFactory beanFactory) {
        this.environment = environment;
        this.beanFactory = beanFactory;
    }

    boolean matches(String value) {
        if (value == null || value.isBlank()) return false;
        URI request;
        try {
            request = URI.create(value);
        } catch (IllegalArgumentException ignored) {
            return false;
        }
        return explicitEndpoints().stream().anyMatch(endpoint -> matches(endpoint, request))
                || discoveredEndpoints().stream().anyMatch(endpoint -> matches(endpoint, request));
    }

    private Collection<URI> explicitEndpoints() {
        if (!(environment instanceof ConfigurableEnvironment configurableEnvironment)) return List.of();
        Stream<URI> serviceEndpoints = StreamSupport.stream(
                        configurableEnvironment.getPropertySources().spliterator(), false)
                .filter(EnumerablePropertySource.class::isInstance)
                .map(EnumerablePropertySource.class::cast)
                .flatMap(source -> java.util.Arrays.stream(source.getPropertyNames())
                        .filter(name -> name.startsWith(ENDPOINT_PREFIX) && name.endsWith(ENDPOINT_SUFFIX))
                        .map(source::getProperty)
                        .filter(String.class::isInstance)
                        .map(String.class::cast)
                        .map(this::uriOrNull)
                        .filter(Objects::nonNull));
        URI baseUrl = uriOrNull(environment.getProperty(BASE_URL));
        return Stream.concat(serviceEndpoints, Stream.ofNullable(baseUrl)).distinct().toList();
    }

    /** All service-discovery instances are trusted according to the configured deployment policy. */
    private Collection<URI> discoveredEndpoints() {
        try {
            Class<?> discoveryClientType = ClassUtils.forName(DISCOVERY_CLIENT, ClassUtils.getDefaultClassLoader());
            Object discoveryClient = beanFactory.getBeanProvider(discoveryClientType).getIfAvailable();
            if (discoveryClient == null) return List.of();
            @SuppressWarnings("unchecked")
            List<String> services = (List<String>) ReflectionUtils.invokeMethod(
                    discoveryClientType.getMethod("getServices"), discoveryClient);
            if (services == null || services.isEmpty()) return List.of();
            return services.stream().flatMap(service -> Stream.concat(
                            logicalServiceEndpoints(service).stream(),
                            instances(discoveryClientType, discoveryClient, service).stream()))
                    .toList();
        } catch (ClassNotFoundException ignored) {
            return List.of();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot read service-discovery endpoints", exception);
        }
    }

    private List<URI> instances(Class<?> discoveryClientType, Object discoveryClient, String service) {
        try {
            Object result = ReflectionUtils.invokeMethod(
                    discoveryClientType.getMethod("getInstances", String.class), discoveryClient, service);
            if (!(result instanceof Collection<?> instances)) return List.of();
            return instances.stream().map(this::instanceUri).filter(Objects::nonNull).toList();
        } catch (NoSuchMethodException exception) {
            throw new IllegalStateException("Cannot read service-discovery instances", exception);
        }
    }

    /** Supports load-balanced requests such as {@code http://bsp-service/...}. */
    private List<URI> logicalServiceEndpoints(String service) {
        if (service == null || service.isBlank()) return List.of();
        return Stream.of(uriOrNull("http://" + service), uriOrNull("https://" + service))
                .filter(Objects::nonNull)
                .toList();
    }

    private URI instanceUri(Object instance) {
        try {
            Object uri = ReflectionUtils.invokeMethod(instance.getClass().getMethod("getUri"), instance);
            return uri instanceof URI value ? value : null;
        } catch (NoSuchMethodException exception) {
            return null;
        }
    }

    private URI uriOrNull(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return URI.create(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private boolean matches(URI endpoint, URI request) {
        if (!equalsIgnoreCase(endpoint.getScheme(), request.getScheme())
                || !equalsIgnoreCase(endpoint.getHost(), request.getHost())
                || port(endpoint) != port(request)) {
            return false;
        }
        String endpointPath = normalizedPath(endpoint.getPath());
        String requestPath = normalizedPath(request.getPath());
        return "/".equals(endpointPath) || requestPath.equals(endpointPath)
                || requestPath.startsWith(endpointPath + "/");
    }

    private int port(URI uri) {
        if (uri.getPort() >= 0) return uri.getPort();
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }

    private String normalizedPath(String path) {
        if (path == null || path.isBlank() || "/".equals(path)) return "/";
        String normalized = path.startsWith("/") ? path : "/" + path;
        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }

    private boolean equalsIgnoreCase(String left, String right) {
        return left != null && right != null && left.toLowerCase(Locale.ROOT).equals(right.toLowerCase(Locale.ROOT));
    }
}
