package vip.isass.framework.nocode.transport;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransportResolverTest {

    @Test
    void selectsLocalThenGrpcThenHttp() {
        TransportResolver resolver = new TransportResolver();

        assertEquals(TransportKind.LOCAL,
                resolver.select(List.of(transport(TransportKind.HTTP, true),
                        transport(TransportKind.GRPC, true),
                        transport(TransportKind.LOCAL, true))).kind());
        assertEquals(TransportKind.GRPC,
                resolver.select(List.of(transport(TransportKind.HTTP, true),
                        transport(TransportKind.GRPC, true))).kind());
        assertEquals(TransportKind.HTTP,
                resolver.select(List.of(transport(TransportKind.HTTP, true),
                        transport(TransportKind.GRPC, false))).kind());
    }

    @Test
    void fallsBackOnlyBeforeRequestWasSentOrForExplicitlyIdempotentOperations() {
        TransportResolver resolver = new TransportResolver();
        Invocation mutating = new Invocation("asset-service", "icon", "add", List.of(), false);
        Invocation query = new Invocation("asset-service", "icon", "findAll", List.of(), true);
        InvocationTransport grpcSentFailure = failing(
                TransportKind.GRPC, new TransportInvocationException("failed", true));
        InvocationTransport grpcPreflightFailure = failing(
                TransportKind.GRPC, new TransportInvocationException("unavailable", false));
        InvocationTransport http = transport(TransportKind.HTTP, true);

        assertThrows(TransportInvocationException.class,
                () -> resolver.invoke(mutating, List.of(grpcSentFailure, http)));
        assertEquals(TransportKind.HTTP,
                resolver.invoke(mutating, List.of(grpcPreflightFailure, http)));
        assertEquals(TransportKind.HTTP,
                resolver.invoke(query, List.of(grpcSentFailure, http)));
    }

    private InvocationTransport failing(TransportKind kind, RuntimeException failure) {
        return new InvocationTransport() {
            public TransportKind kind() { return kind; }
            public boolean available(Invocation invocation) { return true; }
            public Object invoke(Invocation invocation) { throw failure; }
        };
    }

    private InvocationTransport transport(TransportKind kind, boolean available) {
        return new InvocationTransport() {
            @Override
            public TransportKind kind() {
                return kind;
            }

            @Override
            public boolean available(Invocation invocation) {
                return available;
            }

            @Override
            public Object invoke(Invocation invocation) {
                return kind;
            }
        };
    }
}
