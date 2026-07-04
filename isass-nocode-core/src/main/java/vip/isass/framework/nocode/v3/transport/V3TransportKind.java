package vip.isass.framework.nocode.v3.transport;

public enum V3TransportKind {
    LOCAL(0),
    GRPC(1),
    HTTP(2);

    private final int priority;

    V3TransportKind(int priority) {
        this.priority = priority;
    }

    public int priority() {
        return priority;
    }
}
