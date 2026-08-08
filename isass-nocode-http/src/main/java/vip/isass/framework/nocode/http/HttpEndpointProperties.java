package vip.isass.framework.nocode.http;

import org.springframework.core.env.Environment;

import java.net.URI;

/** Reads an explicit remote URL from {@code isass.http.endpoints.<service>.url}. */
public class HttpEndpointProperties {

    private final Environment environment;

    public HttpEndpointProperties(Environment environment) {
        this.environment = environment;
    }

    public URI getUrl(String service) {
        String url = environment.getProperty("isass.http.endpoints." + service + ".url");
        return url == null || url.isBlank() ? null : URI.create(url);
    }
}
