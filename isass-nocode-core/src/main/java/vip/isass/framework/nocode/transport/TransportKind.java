// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.transport;

public enum TransportKind {
    LOCAL(0),
    GRPC(1),
    HTTP(2);

    private final int priority;

    TransportKind(int priority) {
        this.priority = priority;
    }

    public int priority() {
        return priority;
    }
}
