// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.entrypoint.grpc;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

@ConfigurationProperties("isass.entrypoint.grpc")
public class EntrypointGrpcProperties {
    private int serverPort;
    private Map<String, Endpoint> services = new LinkedHashMap<>();

    public int getServerPort() { return serverPort; }
    public void setServerPort(int serverPort) { this.serverPort = serverPort; }
    public Map<String, Endpoint> getServices() { return services; }
    public void setServices(Map<String, Endpoint> services) { this.services = services; }

    public static class Endpoint {
        private String host;
        private int port;
        private boolean plaintext = true;
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        public boolean isPlaintext() { return plaintext; }
        public void setPlaintext(boolean plaintext) { this.plaintext = plaintext; }
    }
}
