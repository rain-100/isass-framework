package vip.isass.framework.nocode.contract;

import java.util.List;

public record ContractDocument(
        int version,
        String contentHash,
        List<ServiceContract> services,
        List<TypeContract> types
) {
    public static final int CURRENT_VERSION = 1;

    public ContractDocument {
        if (version != CURRENT_VERSION) {
            throw new IllegalArgumentException("Unsupported  contract version: " + version);
        }
        contentHash = contentHash == null ? "" : contentHash;
        services = List.copyOf(services == null ? List.of() : services);
        types = List.copyOf(types == null ? List.of() : types);
    }

    public ContractDocument(
            int version,
            String contentHash,
            List<ServiceContract> services
    ) {
        this(version, contentHash, services, List.of());
    }
}
