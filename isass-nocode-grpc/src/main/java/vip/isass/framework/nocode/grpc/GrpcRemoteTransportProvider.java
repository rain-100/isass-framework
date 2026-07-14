package vip.isass.framework.nocode.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.DisposableBean;
import tools.jackson.databind.ObjectMapper;
import vip.isass.framework.nocode.contract.ContractRegistry;
import vip.isass.framework.nocode.contract.ServiceContract;
import vip.isass.framework.nocode.transport.InvocationTransport;
import vip.isass.framework.nocode.transport.RemoteTransportProvider;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Lazily creates one plaintext gRPC channel per configured nocode target. */
public class GrpcRemoteTransportProvider implements RemoteTransportProvider, DisposableBean {

    private final GrpcEndpointProperties properties;
    private final ContractRegistry contracts;
    private final ObjectMapper objectMapper;
    private final Map<String, ManagedChannel> channels = new ConcurrentHashMap<>();

    public GrpcRemoteTransportProvider(
            GrpcEndpointProperties properties,
            ContractRegistry contracts,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.contracts = contracts;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<InvocationTransport> transports(ServiceContract contract) {
        String target = properties.getEndpoints().get(contract.serviceName());
        if (target == null || target.isBlank()) {
            return List.of();
        }
        ManagedChannel channel = channels.computeIfAbsent(target, this::createChannel);
        return List.of(new GrpcClientTransport(channel, contracts, objectMapper));
    }

    private ManagedChannel createChannel(String target) {
        return ManagedChannelBuilder.forTarget(target).usePlaintext().build();
    }

    @Override
    public void destroy() {
        channels.values().forEach(ManagedChannel::shutdown);
        channels.clear();
    }
}
