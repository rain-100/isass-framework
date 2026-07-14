package vip.isass.framework.nocode.http;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/** Remote HTTP base URIs, keyed by generated nocode service name. */
@ConfigurationProperties("isass.framework.nocode.http")
public class HttpEndpointProperties {

    private Map<String, URI> endpoints = new LinkedHashMap<>();

    public Map<String, URI> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(Map<String, URI> endpoints) {
        this.endpoints = endpoints == null ? new LinkedHashMap<>() : new LinkedHashMap<>(endpoints);
    }
}
