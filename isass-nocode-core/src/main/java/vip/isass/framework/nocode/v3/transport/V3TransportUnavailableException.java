package vip.isass.framework.nocode.v3.transport;

public class V3TransportUnavailableException extends IllegalStateException {

    public V3TransportUnavailableException(V3Invocation invocation) {
        super(invocation == null
                ? "No V3 invocation transport is available"
                : "No V3 invocation transport is available for "
                + invocation.serviceName() + "/" + invocation.entityName()
                + "#" + invocation.operationName());
    }
}
