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

    public NocodeInitializationController(NocodeInitializationDataService dataService) {
        this.dataService = dataService;
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
}
