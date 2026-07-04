package vip.isass.framework.nocode.v3.transport;

public interface V3InvocationTransport {

    V3TransportKind kind();

    boolean available(V3Invocation invocation);

    Object invoke(V3Invocation invocation);
}
