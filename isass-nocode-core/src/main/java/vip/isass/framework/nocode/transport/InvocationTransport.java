package vip.isass.framework.nocode.transport;

public interface InvocationTransport {

    TransportKind kind();

    boolean available(Invocation invocation);

    Object invoke(Invocation invocation);
}
