// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.entrypoint.http;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.core.env.Environment;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import java.net.URI;

final class DefaultHttpEndpointResolver implements HttpEndpointResolver {

    private final Environment environment;
    private final ListableBeanFactory beanFactory;

    DefaultHttpEndpointResolver(Environment environment, ListableBeanFactory beanFactory) {
        this.environment = environment;
        this.beanFactory = beanFactory;
    }

    @Override
    public URI resolve(String serviceName) {
        String serviceUrl = environment.getProperty(
                "isass.entrypoint.http.services." + serviceName + ".url");
        if (serviceUrl != null && !serviceUrl.isBlank()) {
            return URI.create(serviceUrl);
        }
        String baseUrl = environment.getProperty("isass.entrypoint.http.base-url");
        if (baseUrl != null && !baseUrl.isBlank()) {
            return URI.create(baseUrl);
        }
        return discover(serviceName);
    }

    private URI discover(String serviceName) {
        try {
            ClassLoader classLoader = ClassUtils.getDefaultClassLoader();
            Class<?> clientType = ClassUtils.forName(
                    "org.springframework.cloud.client.loadbalancer.LoadBalancerClient", classLoader);
            Object client = beanFactory.getBeanProvider(clientType).getIfAvailable();
            if (client == null) return null;
            Object instance = ReflectionUtils.invokeMethod(
                    clientType.getMethod("choose", String.class), client, serviceName);
            if (instance == null) return null;
            Class<?> instanceType = ClassUtils.forName("org.springframework.cloud.client.ServiceInstance", classLoader);
            return (URI) ReflectionUtils.invokeMethod(instanceType.getMethod("getUri"), instance);
        } catch (ClassNotFoundException ignored) {
            return null;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("服务发现失败: " + serviceName, exception);
        }
    }
}
