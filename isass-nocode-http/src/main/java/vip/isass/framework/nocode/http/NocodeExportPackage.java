package vip.isass.framework.nocode.http;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Full-fidelity export document. Sensitive fields are intentionally retained for environment recovery. */
public record NocodeExportPackage(
        int formatVersion,
        String profileCode,
        long exportedAt,
        Map<String, Map<String, List<?>>> services
) {
    public NocodeExportPackage {
        services = services == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(services));
    }
}
