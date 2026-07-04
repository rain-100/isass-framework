package vip.isass.framework.nocode.v3.transport;

import java.util.Comparator;
import java.util.List;

public class V3TransportResolver {

    public V3InvocationTransport select(List<V3InvocationTransport> transports) {
        return select(null, transports);
    }

    public V3InvocationTransport select(
            V3Invocation invocation,
            List<V3InvocationTransport> transports
    ) {
        return transports.stream()
                .filter(transport -> transport.available(invocation))
                .min(Comparator.comparingInt(transport -> transport.kind().priority()))
                .orElseThrow(() -> new V3TransportUnavailableException(invocation));
    }

    public Object invoke(V3Invocation invocation, List<V3InvocationTransport> transports) {
        List<V3InvocationTransport> candidates = transports.stream()
                .filter(transport -> transport.available(invocation))
                .sorted(Comparator.comparingInt(transport -> transport.kind().priority()))
                .toList();
        if (candidates.isEmpty()) {
            throw new V3TransportUnavailableException(invocation);
        }
        V3TransportInvocationException lastFailure = null;
        for (V3InvocationTransport candidate : candidates) {
            try {
                return candidate.invoke(invocation);
            } catch (V3TransportInvocationException failure) {
                lastFailure = failure;
                if (failure.requestSent() && !invocation.idempotent()) {
                    throw failure;
                }
            }
        }
        throw lastFailure == null ? new V3TransportUnavailableException(invocation) : lastFailure;
    }
}
