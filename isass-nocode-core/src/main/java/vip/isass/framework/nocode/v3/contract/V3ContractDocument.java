package vip.isass.framework.nocode.v3.contract;

import java.util.List;

public record V3ContractDocument(
        int version,
        String contentHash,
        List<V3ServiceContract> services,
        List<V3TypeContract> types
) {
    public static final int CURRENT_VERSION = 1;

    public V3ContractDocument {
        if (version != CURRENT_VERSION) {
            throw new IllegalArgumentException("Unsupported V3 contract version: " + version);
        }
        contentHash = contentHash == null ? "" : contentHash;
        services = List.copyOf(services == null ? List.of() : services);
        types = List.copyOf(types == null ? List.of() : types);
    }

    public V3ContractDocument(
            int version,
            String contentHash,
            List<V3ServiceContract> services
    ) {
        this(version, contentHash, services, List.of());
    }
}
