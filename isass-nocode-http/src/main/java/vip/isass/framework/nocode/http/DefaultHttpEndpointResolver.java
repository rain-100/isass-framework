package vip.isass.framework.nocode.http;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import java.net.URI;

/** Resolves an explicit service URL first, then delegates to Spring Cloud service discovery. */
final class DefaultHttpEndpointResolver implements HttpEndpointResolver {

    private final HttpEndpointProperties properties;
    private static final String LOAD_BALANCER_CLIENT =
            "org.springframework.cloud.client.loadbalancer.LoadBalancerClient";
    private static final String SERVICE_INSTANCE = "org.springframework.cloud.client.ServiceInstance";

    private final ListableBeanFactory beanFactory;

    DefaultHttpEndpointResolver(
            HttpEndpointProperties properties,
            ListableBeanFactory beanFactory
    ) {
        this.properties = properties;
        this.beanFactory = beanFactory;
    }

    @Override
    public URI resolve(String service) {
        URI explicitUrl = properties.getUrl(service);
        if (explicitUrl != null) return explicitUrl;

        return discover(service);
    }

    private URI discover(String service) {
        try {
            ClassLoader classLoader = ClassUtils.getDefaultClassLoader();
            Class<?> clientType = ClassUtils.forName(LOAD_BALANCER_CLIENT, classLoader);
            Object client = beanFactory.getBeanProvider(clientType).getIfAvailable();
            if (client == null) return null;

            Object instance = ReflectionUtils.invokeMethod(
                    clientType.getMethod("choose", String.class), client, service);
            if (instance == null) return null;
            Class<?> instanceType = ClassUtils.forName(SERVICE_INSTANCE, classLoader);
            return (URI) ReflectionUtils.invokeMethod(instanceType.getMethod("getUri"), instance);
        } catch (ClassNotFoundException ignored) {
            return null;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot resolve service instance: " + service, exception);
        }
    }
}
