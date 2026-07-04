package vip.isass.framework.nocode.http;

import java.net.URI;

@FunctionalInterface
public interface V3HttpEndpointResolver {

    URI resolve(String serviceName);
}
