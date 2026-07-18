package vip.isass.framework.nocode.http;

import vip.isass.framework.nocode.contract.ServiceContract;
import vip.isass.framework.nocode.transport.InvocationTransport;
import vip.isass.framework.nocode.transport.RemoteTransportProvider;

import java.util.List;

/** Adds HTTP only for contracts whose service has an explicitly configured endpoint. */
public class HttpRemoteTransportProvider implements RemoteTransportProvider {

    private final HttpEndpointResolver endpoints;
    private final HttpClientTransport transport;

    public HttpRemoteTransportProvider(HttpEndpointResolver endpoints, HttpClientTransport transport) {
        this.endpoints = endpoints;
        this.transport = transport;
    }

    @Override
    public List<InvocationTransport> transports(ServiceContract contract) {
        return endpoints.resolve(contract.service()) == null ? List.of() : List.of(transport);
    }
}
