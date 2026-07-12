package vip.isass.framework.nocode.transport;

import java.util.Comparator;
import java.util.List;

public class TransportResolver {

    public InvocationTransport select(List<InvocationTransport> transports) {
        return select(null, transports);
    }

    public InvocationTransport select(
            Invocation invocation,
            List<InvocationTransport> transports
    ) {
        return transports.stream()
                .filter(transport -> transport.available(invocation))
                .min(Comparator.comparingInt(transport -> transport.kind().priority()))
                .orElseThrow(() -> new TransportUnavailableException(invocation));
    }

    public Object invoke(Invocation invocation, List<InvocationTransport> transports) {
        List<InvocationTransport> candidates = transports.stream()
                .filter(transport -> transport.available(invocation))
                .sorted(Comparator.comparingInt(transport -> transport.kind().priority()))
                .toList();
        if (candidates.isEmpty()) {
            throw new TransportUnavailableException(invocation);
        }
        TransportInvocationException lastFailure = null;
        for (InvocationTransport candidate : candidates) {
            try {
                return candidate.invoke(invocation);
            } catch (TransportInvocationException failure) {
                lastFailure = failure;
                if (failure.requestSent() && !invocation.idempotent()) {
                    throw failure;
                }
            }
        }
        throw lastFailure == null ? new TransportUnavailableException(invocation) : lastFailure;
    }
}
