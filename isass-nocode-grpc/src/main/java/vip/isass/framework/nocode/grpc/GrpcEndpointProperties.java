// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.grpc;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/** Remote gRPC targets keyed by generated nocode service name. */
@ConfigurationProperties("isass.framework.nocode.grpc")
public class GrpcEndpointProperties {

    private Map<String, String> endpoints = new LinkedHashMap<>();

    public Map<String, String> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(Map<String, String> endpoints) {
        this.endpoints = endpoints == null ? new LinkedHashMap<>() : new LinkedHashMap<>(endpoints);
    }
}
