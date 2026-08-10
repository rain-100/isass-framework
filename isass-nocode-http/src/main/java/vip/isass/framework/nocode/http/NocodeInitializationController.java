// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.http;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vip.isass.framework.common.web.Resp;

import java.util.List;
import java.util.Map;

/** HTTP import/export endpoint. The service path keeps documents scoped to the local microservice. */
@RestController
@RequestMapping("/{service:[a-zA-Z0-9-]+-service}/init-data")
public class NocodeInitializationController {

    private final NocodeInitializationDataService dataService;
    private final NocodeExportService exportService;

    public NocodeInitializationController(
            NocodeInitializationDataService dataService,
            NocodeExportService exportService
    ) {
        this.dataService = dataService;
        this.exportService = exportService;
    }

    @PostMapping("/import")
    public Resp<NocodeInitializationDataService.ImportResult> importData(
            @PathVariable String service,
            @RequestBody Map<String, List<Map<String, Object>>> document
    ) {
        return Resp.bizSuccess(dataService.importDataWithFailures(service, document));
    }

    @GetMapping("/entities")
    public Resp<List<NocodeInitializationDataService.EntityInfo>> entities(@PathVariable String service) {
        return Resp.bizSuccess(dataService.entities(service));
    }

    @GetMapping("/export")
    public Resp<Map<String, List<?>>> exportData(
            @PathVariable String service,
            @RequestParam("entities") List<String> entities
    ) {
        return Resp.bizSuccess(dataService.exportData(service, entities));
    }

    @GetMapping("/export-profiles")
    public Resp<List<NocodeExportService.ProfileInfo>> exportProfiles() {
        return Resp.bizSuccess(exportService.profiles());
    }

    /**
     * Exports either a reusable profile or caller-supplied entity plans. Each plan's criteria map
     * maps directly to generated Criteria setters and supports ${export.Entity.property} references.
     */
    @PostMapping("/export")
    public Resp<NocodeExportPackage> export(
            @PathVariable String service,
            @RequestBody NocodeExportRequest request
    ) {
        return Resp.bizSuccess(exportService.export(service, request));
    }

    @PostMapping("/import-package")
    public Resp<Map<String, NocodeInitializationDataService.ImportResult>> importPackage(
            @RequestBody NocodeExportPackage document
    ) {
        return Resp.bizSuccess(exportService.importPackage(document));
    }

    @PostMapping("/export-internal")
    public Resp<Map<String, List<?>>> exportInternal(
            @PathVariable String service,
            @RequestBody InternalExportRequest request
    ) {
        return Resp.bizSuccess(exportService.exportInternal(service, request.plans(), request.input()));
    }

    public record InternalExportRequest(List<NocodeExportPlan> plans, Map<String, Object> input) {
    }
}
