package vip.isass.framework.nocode.v3.transport;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class V3TransportResolverTest {

    @Test
    void selectsLocalThenGrpcThenHttp() {
        V3TransportResolver resolver = new V3TransportResolver();

        assertEquals(V3TransportKind.LOCAL,
                resolver.select(List.of(transport(V3TransportKind.HTTP, true),
                        transport(V3TransportKind.GRPC, true),
                        transport(V3TransportKind.LOCAL, true))).kind());
        assertEquals(V3TransportKind.GRPC,
                resolver.select(List.of(transport(V3TransportKind.HTTP, true),
                        transport(V3TransportKind.GRPC, true))).kind());
        assertEquals(V3TransportKind.HTTP,
                resolver.select(List.of(transport(V3TransportKind.HTTP, true),
                        transport(V3TransportKind.GRPC, false))).kind());
    }

    @Test
    void fallsBackOnlyBeforeRequestWasSentOrForExplicitlyIdempotentOperations() {
        V3TransportResolver resolver = new V3TransportResolver();
        V3Invocation mutating = new V3Invocation("asset-service", "icon", "add", List.of(), false);
        V3Invocation query = new V3Invocation("asset-service", "icon", "findAll", List.of(), true);
        V3InvocationTransport grpcSentFailure = failing(
                V3TransportKind.GRPC, new V3TransportInvocationException("failed", true));
        V3InvocationTransport grpcPreflightFailure = failing(
                V3TransportKind.GRPC, new V3TransportInvocationException("unavailable", false));
        V3InvocationTransport http = transport(V3TransportKind.HTTP, true);

        assertThrows(V3TransportInvocationException.class,
                () -> resolver.invoke(mutating, List.of(grpcSentFailure, http)));
        assertEquals(V3TransportKind.HTTP,
                resolver.invoke(mutating, List.of(grpcPreflightFailure, http)));
        assertEquals(V3TransportKind.HTTP,
                resolver.invoke(query, List.of(grpcSentFailure, http)));
    }

    private V3InvocationTransport failing(V3TransportKind kind, RuntimeException failure) {
        return new V3InvocationTransport() {
            public V3TransportKind kind() { return kind; }
            public boolean available(V3Invocation invocation) { return true; }
            public Object invoke(V3Invocation invocation) { throw failure; }
        };
    }

    private V3InvocationTransport transport(V3TransportKind kind, boolean available) {
        return new V3InvocationTransport() {
            @Override
            public V3TransportKind kind() {
                return kind;
            }

            @Override
            public boolean available(V3Invocation invocation) {
                return available;
            }

            @Override
            public Object invoke(V3Invocation invocation) {
                return kind;
            }
        };
    }
}
