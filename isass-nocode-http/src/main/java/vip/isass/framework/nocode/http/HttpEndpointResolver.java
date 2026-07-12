package vip.isass.framework.nocode.http;

import java.net.URI;

@FunctionalInterface
public interface HttpEndpointResolver {

    URI resolve(String serviceName);
}
