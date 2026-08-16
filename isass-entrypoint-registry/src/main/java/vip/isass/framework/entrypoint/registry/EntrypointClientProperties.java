// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.entrypoint.registry;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ConfigurationProperties("isass.entrypoint.client")
public class EntrypointClientProperties {

    private List<String> transportOrder = List.of("HTTP");
    private Map<String, Service> services = new LinkedHashMap<>();

    public List<String> getTransportOrder() {
        return transportOrder;
    }

    public void setTransportOrder(List<String> transportOrder) {
        this.transportOrder = transportOrder;
    }

    public Map<String, Service> getServices() {
        return services;
    }

    public void setServices(Map<String, Service> services) {
        this.services = services;
    }

    public List<String> transportOrder(String serviceName) {
        Service service = services.get(serviceName);
        List<String> configured = service == null ? null : service.getTransportOrder();
        return configured == null || configured.isEmpty() ? transportOrder : configured;
    }

    public static class Service {
        private List<String> transportOrder;

        public List<String> getTransportOrder() {
            return transportOrder;
        }

        public void setTransportOrder(List<String> transportOrder) {
            this.transportOrder = transportOrder;
        }
    }
}
